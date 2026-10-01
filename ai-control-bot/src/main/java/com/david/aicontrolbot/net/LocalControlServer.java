package com.david.aicontrolbot.net;

import com.david.aicontrolbot.entity.ControlBotEntity;
import net.minecraft.server.MinecraftServer;
import net.minecraft.server.level.ServerLevel;
import net.minecraft.server.level.ServerPlayer;
import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.util.Map;

public final class LocalControlServer {
    private static final int PORT = 8765;
    private static HttpServer httpServer;
    private static MinecraftServer server;
    private LocalControlServer() {}
    public static synchronized void start(MinecraftServer s) {
        if (httpServer != null) return;
        server = s;
        try {
            httpServer = HttpServer.create(new InetSocketAddress("127.0.0.1", PORT), 0);
            httpServer.createContext("/command", LocalControlServer::handleCommand);
            httpServer.createContext("/status", LocalControlServer::handleStatus);
            httpServer.setExecutor(java.util.concurrent.Executors.newSingleThreadExecutor());
            httpServer.start();
            System.out.println("[AIControlBot] http://127.0.0.1:" + PORT);
        } catch(IOException e) { server=null; }
    }
    public static synchronized void stop() { if(httpServer!=null)httpServer.stop(0); httpServer=null; server=null; }
    private static void handleCommand(HttpExchange ex) throws IOException {
        if(!"GET".equalsIgnoreCase(ex.getRequestMethod())){send(ex,405,"Only GET");return;}
        Map<String,String> q=parse(ex.getRequestURI()); String cmd=q.getOrDefault("cmd","stop").toLowerCase();
        if(server==null){send(ex,503,"Minecraft not running");return;}
        server.execute(() -> apply(cmd)); send(ex,200,"{\"ok\":true,\"queued\":\"" + escape(cmd) + "\"}");
    }
    private static void handleStatus(HttpExchange ex) throws IOException { send(ex,200,server==null?"{\"running\":false}":"{\"running\":true}"); }
    private static void apply(String cmd) {
        if(server==null)return;
        for(ServerLevel l:server.getAllLevels()) for(ServerPlayer p:server.getPlayerList().getPlayers()) {
            if(p.level()!=l)continue;
            for(ControlBotEntity b:l.getEntitiesOfClass(ControlBotEntity.class,p.getBoundingBox().inflate(64))) switch(cmd) {
                case "forward" -> b.setInput(1,0,8); case "back" -> b.setInput(-1,0,8);
                case "left" -> b.setInput(0,-1,8); case "right" -> b.setInput(0,1,8);
                case "jump" -> b.jumpNow(); case "stop" -> b.stopInput(); case "look" -> b.faceNearestPlayer();
                default -> b.stopInput();
            }
        }
    }
    private static Map<String,String> parse(URI uri) {
        java.util.HashMap<String,String> m=new java.util.HashMap<>(); String q=uri.getRawQuery();
        if(q==null)return m; for(String part:q.split("&")){String[] kv=part.split("=",2);m.put(URLDecoder.decode(kv[0],StandardCharsets.UTF_8),kv.length>1?URLDecoder.decode(kv[1],StandardCharsets.UTF_8):"");} return m;
    }
    private static void send(HttpExchange e,int code,String body)throws IOException{byte[] b=body.getBytes(StandardCharsets.UTF_8);e.getResponseHeaders().set("Content-Type","application/json; charset=utf-8");e.sendResponseHeaders(code,b.length);try(OutputStream o=e.getResponseBody()){o.write(b);}}
    private static String escape(String s){return s.replace("\\","\\\\").replace("\"","\\\"");}
}
