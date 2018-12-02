package Proyecto;

import com.sun.management.OperatingSystemMXBean;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import org.hyperic.sigar.*;

import java.lang.management.ManagementFactory;
import java.net.*;
import java.util.Enumeration;

import static org.hyperic.jni.ArchName.getName;


public class Cliente {
    private int port;
    private static String servidor;
    private static String datos;

    public Cliente(int port, String servidor) {
        this.port = port;
        Cliente.servidor = servidor;
        datos = datos;
        startSending();
    }

    private ClienteHandler clientHandler = new ClienteHandler();

    public void run() throws Exception {
        EventLoopGroup workerGroup = new NioEventLoopGroup();
        try {
            Bootstrap b = new Bootstrap(); // (2)
            b.group(workerGroup)
                    .channel(NioSocketChannel.class) // (3)
                    .handler(new ChannelInitializer<SocketChannel>() { // (4)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addLast(clientHandler);

                        }
                    })
                    .option(ChannelOption.SO_KEEPALIVE, true); // (6)

            // Bind and start to accept incoming connections.
            ChannelFuture f = b.connect("localhost", port).sync(); // (7)

            // Wait until the server socket is closed.
            // In this example, this does not happen, but you can do that to gracefully
            // shut down your server.
            f.channel().closeFuture().sync();
        } finally {
            workerGroup.shutdownGracefully();
        }
    }

    private void startSending() {
        new Thread() {
            public void run() {
                try {
                    String IP,vProcesador, uProcesador, ram, datos, freeram;

                    while (true) {
                        IP = ip();
                        vProcesador = VProcesador();
                        uProcesador = UProcesador();
                        ram = Ram();
                        freeram = RamFree();
                        datos = IP + "," + vProcesador + "," + uProcesador + "," + ram + "," + freeram + ",";
                        Thread.sleep(1000);
                        clientHandler.sendMessage(datos);
                    }
                } catch (InterruptedException e) {
                    e.printStackTrace();
                } catch (SigarException e) {
                    e.printStackTrace();
                } catch (SocketException e) {
                    e.printStackTrace();
                } catch (UnknownHostException e) {
                    e.printStackTrace();
                }
            }
        }.start();
    }

    private static String ip() throws SocketException, UnknownHostException {
        String output = "";
        try(final DatagramSocket socket = new DatagramSocket()){
            socket.connect(InetAddress.getByName("8.8.8.8"), 10002);
            output = socket.getLocalAddress().getHostAddress();
        }
        return output;
    }

    public static void main(String[] args) throws Exception {
        final int port;
        if (args.length > 0) {
            port = Integer.parseInt(args[0]);
        } else {
            port = 5432;
        }
        new Cliente(port, servidor).run();

    }

    //Metodos cliente
    private static String VProcesador() {
        Sigar sigar = new Sigar();
        String output = "";
        CpuInfo[] cpuInfoList = null;
        String[] parts;
        String part2 = null;
        try {
            cpuInfoList = sigar.getCpuInfoList();

            for (CpuInfo info : cpuInfoList) {
                output += info.getVendor();
                output += " ";
                output += info.getMhz();
                parts = output.split(" ");
                part2 = parts[1];
                break;
            }
        } catch (SigarException e) {
        }
        return part2;
    }

    private static String UProcesador() {
        String output = "";
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        output += (osBean.getProcessCpuLoad() * 100) + "%";
        return output;

    }

    private static String Ram() throws SigarException {
        Sigar sigar = new Sigar();
        String output = "";
        int mb = 1024 * 1024;
        int gb = 1024 * 1024 * 1024;
        Mem mem = sigar.getMem();
        output += (mem.getTotal() / gb) + "gb";
        return output;
    }

    private static String RamFree() throws SigarException {
        Sigar sigar = new Sigar();
        String output = "";
        int mb = 1024 * 1024;
        int gb = 1024 * 1024 * 1024;
        Mem freemem = sigar.getMem();
        output += (freemem.getFree() / gb) + "gb";
        return output;
    }
}
