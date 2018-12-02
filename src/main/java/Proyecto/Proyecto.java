package Proyecto;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;

import javax.swing.*;
import java.io.IOException;
import java.lang.management.ManagementFactory;
import java.lang.management.OperatingSystemMXBean;
import java.net.*;
import java.util.ArrayList;

public class Proyecto {
    private ServidorHandler serverHandler = new ServidorHandler();
    //public static ArrayList<client> clientes = new ArrayList<>();
    //public static ArrayList<String> ips = new ArrayList<>();
    public static ArrayList<String> colaClientes = new ArrayList<String>();
    public static boolean soyServidor = true;
    private static int port = 5432;

    public static void main(String[] args) throws Exception {
        do {
            //Se obtiene la ip que tiene mejor rank
            String ipOptima = obtenerIpOptima();
            //Se obtiene la direccion local
            InetAddress direccion = InetAddress.getLocalHost();
            //Se obtiene la ip local
            String miIp = direccion.getHostAddress();
            //Soy servidor si mi procesador no esta saturado y si mi ip es la mas optima
            soyServidor = estadoSaturacion() != true && ipOptima.equals(miIp);
            soyServidor = true;
            //Mientras sea servidor realiza el proceso de servidor
            if (soyServidor) {
                Interfaz gui = new Interfaz();
                gui.setDefaultCloseOperation(JFrame.EXIT_ON_CLOSE);
                gui.setSize(1000, 600);
                gui.pack();
                gui.setVisible(true);
                Proyecto proy = new Proyecto(port);
                proy.run();
                /*
                //Se verifica si aun si la maquina local es servidor verificando la saturacion
                soyServidor = !estadoSaturacion();
                //Se obtiene la ip optima
                ipOptima = obtenerIpOptima();
                if(!soyServidor){
                    //Se envia el nuevo servidor a todas las maquinas
                    enviarNuevoServidor(ipOptima);
                }
                *///Todo esto comentado debe ir dentro de lo que hace el servidor
            } else {
                String servidor = recibirNuevoServidor();
                //if(/*Si apesar que no soy servidor estoy activo*/){
                //
                //f.channel().closeFuture().sync();
                //}
                //Se inicia proceso como cliente
                Cliente cliente = new Cliente(5432, servidor);
                cliente.run();
            }
        } while (true);
    }

    //Metodo de servidor
    public Proyecto(int port) {
        this.port = port;
    }

    //Metodo run de servidor
    public void run() {
        EventLoopGroup bossGroup = new NioEventLoopGroup(); // (1)
        EventLoopGroup workerGroup = new NioEventLoopGroup(); // (2)
        try {
            ServerBootstrap b = new ServerBootstrap(); // (3)
            b.group(bossGroup, workerGroup)  // (4)
                    .channel(NioServerSocketChannel.class) // (5)
                    .childHandler(new ChannelInitializer<SocketChannel>() { // (6)
                        @Override
                        public void initChannel(SocketChannel ch) throws Exception { // (7)
                            ch.pipeline().addLast(serverHandler); // (8)
                        }
                    })
                    .option(ChannelOption.SO_BACKLOG, 128)          // (9)
                    .childOption(ChannelOption.SO_KEEPALIVE, true); // (10)
            ChannelFuture f = null; // (11)
            try {
                f = b.bind(port).sync();
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
            try {
                f.channel().closeFuture().sync(); // (12)
            } catch (InterruptedException e1) {
                e1.printStackTrace();
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    //Metodo para obtener la ip que sera el servidor
    private static String obtenerIpOptima() throws UnknownHostException {
        String ipOptima;
        //Proceso de la tabla de rankeo para obtener ipOptima
        InetAddress direccion = InetAddress.getLocalHost();
        ipOptima = direccion.getHostAddress();
        return ipOptima;
    }

    //Metodo para obtener el estado de saturacion local
    private static boolean estadoSaturacion() {
        boolean estado;
        OperatingSystemMXBean osBean = ManagementFactory.getPlatformMXBean(OperatingSystemMXBean.class);
        long usoProcesador = (long) (osBean.getSystemLoadAverage() * 100);
        estado = colaClientes.size() > 50 && usoProcesador > 90;
        return estado;
    }

    //
    //
    //Se puede realizar con netty (Adaptar)
    //
    //
    private static void enviarNuevoServidor(String ipOptima) throws IOException {
        MulticastSocket enviador = new MulticastSocket();
        // El dato que queramos enviar en el mensaje, como array de bytes.
        byte[] dato = ipOptima.getBytes();

        // Usamos la direccion Multicast 230.0.0.1, por poner alguna dentro del rango
        // y el puerto 55557, uno cualquiera que esté libre.
        DatagramPacket dgp = new DatagramPacket(dato, dato.length, InetAddress.getByName("127.198.1.0"), 5431);

        // Envío
        enviador.send(dgp);
    }

    //
    //
    //Se puede realizar con netty (Adaptar)
    //
    //
    private static String recibirNuevoServidor() throws IOException {
        String ipOptima;
        // El mismo puerto que se uso en la parte de enviar.
        MulticastSocket escucha = new MulticastSocket(5431);

        // Nos ponemos a la escucha de la misma IP de Multicast que se uso en la parte de enviar.
        escucha.joinGroup(InetAddress.getByName("127.198.1.0"));

        // Un array de bytes con tamaño suficiente para recoger el mensaje enviado,
        // bastaría con 4 bytes.
        byte[] dato = new byte[5120];

        // Se espera la recepción. La llamada a receive() se queda
        // bloqueada hasta que llegue un mensaje.
        DatagramPacket dgp = new DatagramPacket(dato, dato.length);
        escucha.receive(dgp);

        // Obtención del dato ya relleno.
        dato = dgp.getData();
        ipOptima = new String(dato, 0, dgp.getLength());
        return ipOptima;
    }

}