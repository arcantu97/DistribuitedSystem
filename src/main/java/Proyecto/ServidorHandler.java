package Proyecto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import javax.swing.table.DefaultTableModel;
import java.nio.charset.Charset;

@ChannelHandler.Sharable
public class ServidorHandler extends ChannelInboundHandlerAdapter {
    ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);
    private String ip;
    private String so="";
    private String vProcesador;
    private String uProcesador;
    private String ram;
    private String freeram;
    private int cont=0;

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        String text = buf.toString(Charset.defaultCharset());
        System.out.print(text + "\n");
        String[] datos = text.split(",");
        ip = datos[0];
        vProcesador = datos[1];
        uProcesador = datos[2];
        ram = datos[3];
        freeram = datos[4];
        DefaultTableModel modelo = (DefaultTableModel) Interfaz.jTable1.getModel();
        for (Channel channel : clients) {
            if (!ctx.channel().equals(channel)) {
                ByteBuf bufToSend = ctx.alloc().buffer();
                bufToSend.writeCharSequence(text, Charset.defaultCharset());
                channel.writeAndFlush(bufToSend);
            }
            else if(cont<1){
                modelo.addRow(new Object[]{ip, so, vProcesador, uProcesador, ram, freeram});
                cont++;
            }
            System.out.println("Esta activa la conexion!");
            Interfaz.actualizarfilas(ip, uProcesador);
        }
        ;
        buf.release();

    }

        @Override
        public void channelActive(ChannelHandlerContext ctx) throws Exception {
            clients.add(ctx.channel());
        }

        @Override
        public void channelInactive(ChannelHandlerContext ctx) throws Exception {
            clients.remove(ctx.channel());
            Interfaz.eliminarfilas(ip);
        }

}
