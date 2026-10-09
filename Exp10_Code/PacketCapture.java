import java.lang.foreign.Arena;
import java.lang.foreign.FunctionDescriptor;
import java.lang.foreign.Linker;
import java.lang.foreign.MemorySegment;
import java.lang.foreign.SymbolLookup;
import java.lang.foreign.ValueLayout;
import java.lang.invoke.MethodHandle;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.Map;
import java.util.TreeMap;

/**
 * PacketCapture - a packet sniffer built on a Linux raw socket
 * (AF_PACKET, SOCK_RAW), called directly from Java through the
 * Foreign Function & Memory API. No external library is used.
 *
 * Usage: sudo java PacketCapture [packet_count] [-x]
 *        packet_count : packets to capture (default 20, 0 = until Ctrl+C)
 *        -x           : also print a hex dump of every frame
 */

public class PacketCapture {

    static final int AF_PACKET = 17;
    static final int SOCK_RAW = 3;
    static final int ETH_P_ALL = 0x0003;
    static final int FRAME_SIZE = 65536;

    static final Map<String, Integer> stats = new TreeMap<>();
    static int total = 0;

    public static void main(String[] args) throws Throwable {

        int maxPackets = 20;
        boolean hexDump = false;

        for (String a : args) {
            if (a.equals("-x"))
                hexDump = true;
            else
                maxPackets = Integer.parseInt(a);
        }

        Linker linker = Linker.nativeLinker();
        SymbolLookup libc = linker.defaultLookup();

        // int socket(int domain, int type, int protocol)
        MethodHandle socket = linker.downcallHandle(
            libc.find("socket").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT
            )
        );

        // ssize_t recvfrom(int fd, void *buf, size_t len, int flags,
        //                  struct sockaddr *src, socklen_t *addrlen)
        MethodHandle recvfrom = linker.downcallHandle(
            libc.find("recvfrom").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.JAVA_LONG,
                ValueLayout.JAVA_INT,
                ValueLayout.ADDRESS,
                ValueLayout.ADDRESS
            )
        );

        // int close(int fd)
        MethodHandle close = linker.downcallHandle(
            libc.find("close").orElseThrow(),
            FunctionDescriptor.of(
                ValueLayout.JAVA_INT,
                ValueLayout.JAVA_INT
            )
        );

        // htons(ETH_P_ALL) = 0x0300 on little-endian CPUs.
        int proto = Short.toUnsignedInt(
            Short.reverseBytes((short) ETH_P_ALL)
        );

        int fd = (int) socket.invokeExact(
            AF_PACKET, SOCK_RAW, proto
        );

        if (fd < 0) {
            System.err.println(
                "socket() failed - raw sockets need root. Run with sudo."
            );
            System.exit(1);
        }

        Runtime.getRuntime().addShutdownHook(
            new Thread(PacketCapture::printStats)
        );

        System.out.println(
            "Capturing on all interfaces (fd=" + fd +
            "). Press Ctrl+C to stop.\n"
        );

        try (Arena arena = Arena.ofConfined()) {

            MemorySegment buf = arena.allocate(FRAME_SIZE);

            while (maxPackets == 0 || total < maxPackets) {

                long n = (long) recvfrom.invokeExact(
                    fd,
                    buf,
                    (long) FRAME_SIZE,
                    0,
                    MemorySegment.NULL,
                    MemorySegment.NULL
                );

                if (n <= 0)
                    continue;

                byte[] frame = buf.asSlice(0, n)
                    .toArray(ValueLayout.JAVA_BYTE);

                total++;

                System.out.printf(
                    "#%-4d %s len=%d%n",
                    total,
                    LocalTime.now().format(
                        DateTimeFormatter.ofPattern("HH:mm:ss.SSS")
                    ),
                    n
                );

                decode(frame);

                if (hexDump)
                    hexDump(frame);

                System.out.println();
            }
        }

        // Close the raw socket.
        int r = (int) close.invokeExact(fd);
    }

    // ------------------------------------------------------------
    // Ethernet decoding
    // ------------------------------------------------------------

    static void decode(byte[] f) {

        if (f.length < 14) {
            count("Truncated");
            return;
        }

        System.out.println(
            " ETH " + mac(f, 6) + " -> " + mac(f, 0)
        );

        int type = u16(f, 12);
        int off = 14;

        if (type == 0x8100 && f.length >= 18) {
            // 802.1Q VLAN tag
            type = u16(f, 16);
            off = 18;
        }

        switch (type) {

            case 0x0800 -> decodeIPv4(f, off);

            case 0x86DD -> decodeIPv6(f, off);

            case 0x0806 -> decodeArp(f, off);

            default -> {
                count("Other");
                System.out.printf(
                    " Ethertype 0x%04X%n", type
                );
            }
        }
    }

    // ------------------------------------------------------------
    // IPv4 decoding
    // ------------------------------------------------------------

    static void decodeIPv4(byte[] f, int o) {

        if (f.length < o + 20) {
            count("Truncated");
            return;
        }

        int ihl = (f[o] & 0x0F) * 4;
        int ttl = u8(f, o + 8);
        int proto = u8(f, o + 9);

        System.out.printf(
            " IPv4 %s -> %s ttl=%d total_len=%d%n",
            ip4(f, o + 12),
            ip4(f, o + 16),
            ttl,
            u16(f, o + 2)
        );

        decodeL4(f, o + ihl, proto);
    }

    // ------------------------------------------------------------
    // IPv6 decoding
    // ------------------------------------------------------------

    static void decodeIPv6(byte[] f, int o) {

        if (f.length < o + 40) {
            count("Truncated");
            return;
        }

        System.out.printf(
            " IPv6 %s -> %s hop_limit=%d%n",
            ip6(f, o + 8),
            ip6(f, o + 24),
            u8(f, o + 7)
        );

        decodeL4(f, o + 40, u8(f, o + 6));
    }

    // ------------------------------------------------------------
    // ARP decoding
    // ------------------------------------------------------------

    static void decodeArp(byte[] f, int o) {

        if (f.length < o + 28) {
            count("Truncated");
            return;
        }

        count("ARP");

        String op = u16(f, o + 6) == 1
            ? "request"
            : "reply";

        System.out.printf(
            " ARP %s %s (%s) -> %s%n",
            op,
            ip4(f, o + 14),
            mac(f, o + 8),
            ip4(f, o + 24)
        );
    }

    // ------------------------------------------------------------
    // TCP, UDP and ICMP decoding
    // ------------------------------------------------------------

    static void decodeL4(byte[] f, int o, int proto) {

        switch (proto) {

            case 6 -> {
                // TCP

                if (f.length < o + 20) {
                    count("Truncated");
                    return;
                }

                count("TCP");

                int fl = u8(f, o + 13);
                StringBuilder flags = new StringBuilder();

                if ((fl & 0x02) != 0)
                    flags.append("SYN ");

                if ((fl & 0x10) != 0)
                    flags.append("ACK ");

                if ((fl & 0x01) != 0)
                    flags.append("FIN ");

                if ((fl & 0x04) != 0)
                    flags.append("RST ");

                if ((fl & 0x08) != 0)
                    flags.append("PSH ");

                System.out.printf(
                    " TCP %d -> %d seq=%d win=%d [%s]%n",
                    u16(f, o),
                    u16(f, o + 2),
                    u32(f, o + 4),
                    u16(f, o + 14),
                    flags.toString().trim()
                );
            }

            case 17 -> {
                // UDP

                if (f.length < o + 8) {
                    count("Truncated");
                    return;
                }

                count("UDP");

                System.out.printf(
                    " UDP %d -> %d len=%d%n",
                    u16(f, o),
                    u16(f, o + 2),
                    u16(f, o + 4)
                );
            }

            case 1, 58 -> {
                // ICMP / ICMPv6

                if (f.length < o + 4) {
                    count("Truncated");
                    return;
                }

                count(proto == 1 ? "ICMP" : "ICMPv6");

                System.out.printf(
                    " %s type=%d code=%d%n",
                    proto == 1 ? "ICMP" : "ICMPv6",
                    u8(f, o),
                    u8(f, o + 1)
                );
            }

            default -> {
                count("Other");

                System.out.println(
                    " IP protocol number " + proto
                );
            }
        }
    }

    // ------------------------------------------------------------
    // Helper functions
    // ------------------------------------------------------------

    static int u8(byte[] b, int i) {
        return b[i] & 0xFF;
    }

    static int u16(byte[] b, int i) {
        return (u8(b, i) << 8) | u8(b, i + 1);
    }

    static long u32(byte[] b, int i) {
        return ((long) u16(b, i) << 16)
            | u16(b, i + 2);
    }

    static String mac(byte[] b, int i) {
        return String.format(
            "%02x:%02x:%02x:%02x:%02x:%02x",
            u8(b, i),
            u8(b, i + 1),
            u8(b, i + 2),
            u8(b, i + 3),
            u8(b, i + 4),
            u8(b, i + 5)
        );
    }

    static String ip4(byte[] b, int i) {
        return u8(b, i) + "."
            + u8(b, i + 1) + "."
            + u8(b, i + 2) + "."
            + u8(b, i + 3);
    }

    static String ip6(byte[] b, int i) {

        StringBuilder s = new StringBuilder();

        for (int k = 0; k < 8; k++) {

            if (k > 0)
                s.append(':');

            s.append(
                Integer.toHexString(u16(b, i + 2 * k))
            );
        }

        return s.toString();
    }

    static void count(String k) {
        stats.merge(k, 1, Integer::sum);
    }

    // ------------------------------------------------------------
    // Hexadecimal dump
    // ------------------------------------------------------------

    static void hexDump(byte[] f) {

        int len = Math.min(f.length, 128);

        for (int i = 0; i < len; i += 16) {

            StringBuilder hex = new StringBuilder();
            StringBuilder asc = new StringBuilder();

            for (int j = i; j < i + 16 && j < len; j++) {

                hex.append(
                    String.format("%02x ", f[j] & 0xFF)
                );

                asc.append(
                    f[j] >= 32 && f[j] < 127
                        ? (char) f[j]
                        : '.'
                );
            }

            System.out.printf(
                "     %04x %-48s %s%n",
                i,
                hex,
                asc
            );
        }
    }

    // ------------------------------------------------------------
    // Capture summary
    // ------------------------------------------------------------

    static void printStats() {

        System.out.println(
            "\n---- Capture summary: " + total + " packets ----"
        );

        stats.forEach(
            (k, v) -> System.out.printf(
                " %-10s %d%n", k, v
            )
        );
    }
}
