import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMember;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMembersCount;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.ChatMember;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.List;

public class Bot extends TelegramLongPollingCommandBot {
    public static void main(String[] args) {
        ApiContextInitializer.init();
        TelegramBotsApi telegramBotsApi = new TelegramBotsApi();
        try {
            telegramBotsApi.registerBot(new Bot());
        } catch (TelegramApiRequestException e) {
            e.printStackTrace();
        }
    }


    private Bot() {
        super(getBotOptions(), "mafia_az_bot");
    }

    private static DefaultBotOptions getBotOptions() {
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
//        botOptions.setProxyHost("80.211.29.222");
//        botOptions.setProxyPort(8975);
//        botOptions.setProxyType(DefaultBotOptions.ProxyType.SOCKS5);
        botOptions.setProxyHost("45.84.224.17");
        botOptions.setProxyPort(3128);
        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
        return botOptions;
    }

    private void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableMarkdown(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);

        try {
            sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }

    }

    @Override
    public void onUpdatesReceived(List<Update> updates) {
        Message message = updates.get(0).getMessage();
        if (message != null && message.hasText()) {
            switch (message.getText()) {
                case "/hi":
                    sendMsg(message, "hello");
                    break;
                case "/test":
                    GetChatMembersCount getChatMembersCount = new GetChatMembersCount();
                    getChatMembersCount.setChatId(message.getChatId());
                    try {
                        sendMsg(message, "В чате " + sendApiMethod(getChatMembersCount) + " юзеров");
                    } catch (TelegramApiException e) {
                        sendMsg(message, e.getMessage());
                    }
                    break;
            }
        }
    }

    @Override
    public void processNonCommandUpdate(Update update) {

    }

    public String getBotToken() {
        return "1042713529:AAED4qAkvX16PLynfTJ8K2tmiYwbU8-VKDY";
    }
}
