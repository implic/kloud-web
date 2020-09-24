package kloud.backend.service.dto;

import io.kubernetes.client.Exec;
import io.kubernetes.client.openapi.ApiException;
import kloud.backend.service.dto.ConsoleSize;
import kloud.backend.service.dto.Pod;
import org.springframework.web.socket.TextMessage;
import org.springframework.web.socket.WebSocketSession;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;


public class WsConnection implements Runnable {

    private InputStream inputStream;
    private OutputStream outputStream;
    private Exec exec;
    private WebSocketSession session;
    private Pod pod;
    private ConsoleSize consoleSize;
    private Boolean tryBash = false;


    public WsConnection(Map<String, String> stringStringMap, WebSocketSession session) {
        this.setSession(session);
        this.setExec(new Exec());
        this.setPod(new Pod(stringStringMap.get("name"), stringStringMap.get("namespace"), stringStringMap.get("container")));
        this.setConsoleSize(new ConsoleSize(stringStringMap.get("cols"), stringStringMap.get("rows")));
    }

    @Override
    public void run() {
        List<String> cmds = Arrays.asList("/bin/bash", "/bin/sh");
        cmds.forEach((s) -> {
            if (!tryBash) {
                startProcess(s);
            }
        });

    }

    private void startProcess(String shellPath) {
        String namespace = this.getPod().getNamespace();
        String name = this.getPod().getName();
        String container = this.getPod().getContainer();
        Boolean tty = true;
        Boolean initValid = true;
        try {
            Process proc = exec.exec(namespace, name, new String[]{shellPath}, container, true, tty);
            outputStream = proc.getOutputStream();
            inputStream = proc.getInputStream();
            String width = Optional.ofNullable(consoleSize.getCols()).map(s -> "COLUMNS=" + s).orElse("");
            String height = Optional.ofNullable(consoleSize.getRows()).map(s -> "LINES=" + s).orElse("");
            String export = "";
            if (!"".equals(width + height)) {
                export = "export " + width + height + ";";
            }
            String cmdArgs = export + shellPath + "\nclear\n";
            outputStream.write(cmdArgs.getBytes());
            try {
                while (true) {
                    byte[] data = new byte[1024];
                    if (inputStream.read(data) != -1) {
                        TextMessage textMessage = new TextMessage(data);
                        if (initValid && isValidBash(textMessage, shellPath)) {
                            break;
                        } else {
                            tryBash = true;
                            initValid = false;
                        }
                        session.sendMessage(textMessage);
                    }
                }
            } catch (IOException e) {
                System.out.println("Pipe closed");
            } finally {
                proc.destroy();
                System.out.println("session closed... exit thread");
            }


        } catch (ApiException | IOException e) {
            e.printStackTrace();
            try {
                System.out.println("ApiException or IOException... close session");
                session.close();
            } catch (IOException e1) {
                e1.printStackTrace();
            }
        }


    }

    //验证shell
    private boolean isValidBash(TextMessage textMessage, String shellPath) {
        String failMessage = "OCI runtime exec failed";
        return textMessage.getPayload().trim().contains(failMessage);
    }


    //退出 Process
    public void exit() {
        try {
            outputStream.write("exit\nexit\n".getBytes());
        } catch (IOException ignored) {
        }
/*        proc.destroyForcibly();
        try {
            outputStream.close();
            inputStream.close();
        } catch (IOException e) {
            e.printStackTrace();
        }*/

    }


    public InputStream getInputStream() {
        return inputStream;
    }

    public void setInputStream(InputStream inputStream) {
        this.inputStream = inputStream;
    }

    public OutputStream getOutputStream() {
        return outputStream;
    }

    public void setOutputStream(OutputStream outputStream) {
        this.outputStream = outputStream;
    }

    public Exec getExec() {
        return exec;
    }

    public void setExec(Exec exec) {
        this.exec = exec;
    }

    public WebSocketSession getSession() {
        return session;
    }

    public void setSession(WebSocketSession session) {
        this.session = session;
    }


    public Pod getPod() {
        return pod;
    }

    public void setPod(Pod pod) {
        this.pod = pod;
    }

    public ConsoleSize getConsoleSize() {
        return consoleSize;
    }

    public void setConsoleSize(ConsoleSize consoleSize) {
        this.consoleSize = consoleSize;
    }


}
