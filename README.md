```
@Component
@ServerEndpoint(port = 8192, path = "/im", readerIdleTime = 60, writeIdleTime = 30, accepts = 500)
public class WebSocketServer {
    private Logger logger = LoggerFactory.getLogger(WebSocketServer.class);
    private final Gson gson = new Gson();

    private final Map<String, Session> sessions = new ConcurrentHashMap<String, Session>();

    @Reference // dubbo
    private CardService cardService;

    @OnOpen
    public void onOpen(Session session, HttpHeaders headers, @URI String requestUri, @SubProtocol String subProtocol) {
        logger.info("onOpen session: " + session.id() + ", uri: " + requestUri + ", subProtocol: " + subProtocol);
        sessions.put(session.id().asLongText(), session);
    }

    @OnClose
    public void onClose(Session session) throws Exception {
        logger.info("onClose session: " + session.id());
        sessions.remove(session.id().asLongText());

    }

    @OnError
    public void onError(Session session, Throwable throwable) {
        throwable.printStackTrace();
    }

    @OnMessage
    public void onMessage(Session session, String message) {
        logger.info("onMessage session: " + session.id() + message);
        String date = new Date().toString();
        session.sendText("现在时刻: " + date + "发送了:" + message);
        // 这里对于耗时io的逻辑，可以建一个共享线程池，
        String res = null;
        try {
            res = cardService.beginCardGame(1);
        } catch (Throwable err) {
            logger.error("cardService.beginCardGame(1)", err);
        }
        logger.info("onMessage  cardService.beginCardGame result: " + res);

        Request request = gson.fromJson(message, Request.class);
        if (request.getMsgTag() == 10010384) {
            logger.info("onMessage  广播消息 ");
            for (Session s : sessions.values()) {
                s.sendText("广播了：" + message);
            }
        }
    }

    @OnBinary
    public void onBinary(Session session, byte[] bytes) {
        logger.info("onBinary session: " + session.id() + "，收到二进制消息: " + bytes.toString());
        session.sendBinary(bytes);
    }

    @OnEvent
    public void onEvent(Session session, Object evt) {
        if (evt instanceof IdleStateEvent) {
            IdleStateEvent idleStateEvent = (IdleStateEvent) evt;
            switch (idleStateEvent.state()) {
                case READER_IDLE:
                    logger.info("read idle");
                    session.close();
                    break;
                case WRITER_IDLE:
                    logger.info("write idle");
                    break;
                case ALL_IDLE:
                    logger.info("all idle");
                    break;
                default:
                    break;
            }
        }
    }
}


```