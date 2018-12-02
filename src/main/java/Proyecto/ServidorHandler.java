package Proyecto;

import io.netty.buffer.ByteBuf;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandler;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInboundHandlerAdapter;
import io.netty.channel.group.ChannelGroup;
import io.netty.channel.group.DefaultChannelGroup;
import io.netty.util.concurrent.GlobalEventExecutor;

import java.nio.charset.Charset;

@ChannelHandler.Sharable
public class ServidorHandler extends ChannelInboundHandlerAdapter {
    ChannelGroup clients = new DefaultChannelGroup(GlobalEventExecutor.INSTANCE);

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        ByteBuf buf = (ByteBuf) msg;
        String text = buf.toString(Charset.defaultCharset());
        System.out.print(text + "\n");
        for (Channel channel : clients) {
            if (!ctx.channel().equals(channel)) {
                ByteBuf bufToSend = ctx.alloc().buffer();
                bufToSend.writeCharSequence(text, Charset.defaultCharset());
                channel.writeAndFlush(bufToSend);
            }
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
        }

}
