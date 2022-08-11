package io.le.proxy.server.server.handler.socks5;

import io.le.proxy.server.server.config.UsernamePasswordAuth;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.handler.codec.socksx.v5.*;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class Socks5PasswordAuthRequestHandler extends SimpleChannelInboundHandler<DefaultSocks5PasswordAuthRequest> {
    private final UsernamePasswordAuth usernamePasswordAuth;

    public Socks5PasswordAuthRequestHandler(UsernamePasswordAuth usernamePasswordAuth) {
        this.usernamePasswordAuth = usernamePasswordAuth;
    }

    @Override
    protected void channelRead0(ChannelHandlerContext ctx, DefaultSocks5PasswordAuthRequest msg) throws Exception {
        if(usernamePasswordAuth.getUsername().equals(msg.username()) && usernamePasswordAuth.getPassword().equals(msg.password())) {
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.SUCCESS);
            ctx.writeAndFlush(passwordAuthResponse);
            ctx.pipeline().remove(Socks5PasswordAuthRequestDecoder.class);
            ctx.pipeline().remove(ctx.name());
        } else {
            Socks5PasswordAuthResponse passwordAuthResponse = new DefaultSocks5PasswordAuthResponse(Socks5PasswordAuthStatus.FAILURE);
            ctx.writeAndFlush(passwordAuthResponse).addListener(ChannelFutureListener.CLOSE);
        }
    }
}
