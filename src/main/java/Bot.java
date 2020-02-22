import org.telegram.telegrambots.ApiContextInitializer;
import org.telegram.telegrambots.bots.DefaultBotOptions;
import org.telegram.telegrambots.extensions.bots.commandbot.TelegramLongPollingCommandBot;
import org.telegram.telegrambots.meta.ApiContext;
import org.telegram.telegrambots.meta.TelegramBotsApi;
import org.telegram.telegrambots.meta.api.methods.groupadministration.GetChatMembersCount;
import org.telegram.telegrambots.meta.api.methods.send.SendMessage;
import org.telegram.telegrambots.meta.api.objects.Message;
import org.telegram.telegrambots.meta.api.objects.Update;
import org.telegram.telegrambots.meta.exceptions.TelegramApiException;
import org.telegram.telegrambots.meta.exceptions.TelegramApiRequestException;

import java.util.ArrayList;
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

    private List<Game> games = new ArrayList<>();

    private Game findGameById(Long id) {
        for (Game game : games) {
            if (game.getChat().getId().equals(id)) {
                return game;
            }
        }
        return null;
    }

    private Bot() {
        super(getBotOptions(), "mafia_az_bot");
    }

    private static DefaultBotOptions getBotOptions() {
        DefaultBotOptions botOptions = ApiContext.getInstance(DefaultBotOptions.class);
        botOptions.setProxyHost("45.84.224.17");
        botOptions.setProxyPort(3128);
        botOptions.setProxyType(DefaultBotOptions.ProxyType.HTTP);
        return botOptions;
    }

    private void sendMsg(Message message, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        sendMessage.setChatId(message.getChatId().toString());
        sendMessage.setReplyToMessageId(message.getMessageId());
        sendMessage.setText(text);
        try {
            sendApiMethod(sendMessage);
        } catch (TelegramApiException e) {
            e.printStackTrace();
        }
    }

    private void sendMsg(Long chatId, String text) {
        SendMessage sendMessage = new SendMessage();
        sendMessage.enableHtml(true);
        sendMessage.setChatId(chatId);
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
            String[] strings = message.getText().split("[\\s@]+?", 2);
            String command = strings[0];
            String argument;
            if (strings.length > 1) {
                argument = strings[1];
            } else {
                argument = null;
            }

            switch (command) {
                case "/count":
                    sendMsg(message, "В чате " + getCountMembers(message) + " человек");
                    break;
                case "/newGame":
                    newGame(message);
                    break;
                case "/enterGame":
                    enterGame(message, argument);
                    break;
                case "/closeInvite":
                    Game game = closeInvite(message);
                    if (game != null) {
                        sendRoles(game);
                        nextStep(message.getChatId());
                    }
                    break;
                case "/closeGame":
                    games.remove(findGameById(message.getChatId()));
                    sendMsg(message, "Если тут была игра, то, только что ее не стало");
                    break;
                case "/rand":
                    sendMsg(message.getChatId(), "rand: " + ((int) (Math.random() *
                            (Long.parseLong(argument != null ? argument : "2")))));
                    break;
                case "/kill":
                    kill(message.getChatId(), argument != null ? argument : "");
                    break;
                case "/vote":
                    vote(message, argument);
            }
        }
    }

    private void vote(Message message, String name) {
        Game game = findGameById(message.getChatId());
        if (game != null) {
            Player player = game.getPlayerByName(name);
            if (player == null) {
                sendMsg(message, "Не нашел такого, проголосуй заново");
            } else {
                player.votes++;
                game.votesCount++;
                sendMsg(message, message.getFrom().getFirstName() + " полагает, что мафия "
                        + player.getUser().getFirstName());
                if (game.votesCount >= game.countPlayers()) {
                    sendMsg(message.getChatId(), "Все проголосовали!");
                    game.votesCount = 0;
                    Player votedPlayer = game.getPlayerWithMaxCountVotes();
                    if (votedPlayer == null) {
                        sendMsg(message.getChatId(), "Неоднозначные выводы, придется переголосовать");
                    } else {
                        game.setPlayerToPrison(votedPlayer);
                        sendMsg(message.getChatId(), "Коллективный разум посадил в тюрьму "
                                + votedPlayer.getUser().getFirstName() + " (" + votedPlayer.getRole() + ")");
                        nextStep(message.getChatId());
                    }
                }
            }
        }
    }

    private void kill(Long chatId, String argument) {
        String[] strings = argument.split("[\\s]+?", 2);
        if (strings.length < 2) {
            sendMsg(chatId, "Что-то ты не так ввел, должен быть айди игры и имя игрока после команды /kill");
            return;
        }
        String name = strings[1];
        Long gameChatId = Long.parseLong(strings[0]);
        Game game = findGameById(gameChatId);
        if (game == null) {
            sendMsg(chatId, "Неправильный id игры");
            return;
        }
        Player deadPlayer = game.kill(name);
        if (deadPlayer != null) {
            sendMsg(chatId, "Ты коварно убил " + name);
            game.setCitizenStep();
            sendMsg(gameChatId, "Город просыпается, но не весь... \nЭтой ночью убили " + name
                    + " (" + deadPlayer.getRole() + ")");
            sendMsg(gameChatId, "Время обсудить и решить кто мафия, когда определитесь с выбором," +
                    " голосуйте здесь при помощи команды \n<code>/vote Имя</code>");
        } else {
            sendMsg(chatId, "Что-то пошло не так, либо неправильно ввел имя, либо еще не время убивать");
        }
    }

    private void nextStep(Long chatId) {
        Game game = findGameById(chatId);
        if (game == null) {
            return;
        }
        sendMsg(chatId, "Город засыпает, просыпается мафия...");
        game.setMafiaStep();
        Player mafia = game.getMafia();
        if (mafia == null) {
            sendMsg(chatId, "Ой, не просыпается, горожане победили! Чтобы сыграть еще раз," +
                    " воспользуйтесь командой\n /newGame");
            games.remove(findGameById(chatId));
        } else if (game.countPlayers() == 1) {
            sendMsg(chatId, "А убивать-то больше некого, мафия победила! Чтобы сыграть еще раз," +
                    " воспользуйтесь командой\n /newGame");
            games.remove(findGameById(chatId));
        } else {
            sendMsg(mafia.getChatId(), "Время творить зло! Выбери кого убьешь этой ночью " +
                    "при помощи команды <code>/kill " + chatId + "</code> Имя");
        }
    }

    private void sendRoles(Game game) {
        for (Player player : game.initRoles()) {
            sendMsg(player.getChatId(), "Твоя роль " + player.getRole());
        }
    }

    private Game closeInvite(Message message) {
        Game game = findGameById(message.getChatId());
        if (game == null) {
            sendMsg(message, "Для того, чтобы закрыть набор игроков, неплохо бы для начала его открыть");
        } else {
            if (game.countPlayers() < 2) {
                sendMsg(message, "Нужно набрать больше участников, а то игры не выйдет");
            } else {
                game.closeInvite();
                sendMsg(message, "Набор игроков закрыт, сейчас разошлю всем игровые роли");
                return game;
            }
        }
        return null;
    }

    private void enterGame(Message message, String argument) {
        if (argument == null) {
            sendMsg(message, "Нужно указать id игры");
            return;
        }
        Long chatId = new Long(argument);
        Game game = findGameById(chatId);
        if (game == null) {
            sendMsg(message, "Извини, но такой игры нет");
            return;
        }
        if (game.getChat().getId().equals(message.getChatId())) {
            sendMsg(message, "Это нужно делать в личных сообщениях со мной");
            return;
        }
        if (game.addPlayer(message.getFrom(), message.getChatId())) {
            sendMsg(message, "Ты успешно присоединился к игре, возвращайся в чат игры");
            sendMsg(game.getChat().getId(), message.getFrom().getFirstName() +
                    " присоединилась/ся к игре! Теперь в ней " + game.countPlayers() + " игроков!");
        } else {
            sendMsg(message, "Не удалось присоединиться к игре");
        }
    }

    private int getCountMembers(Message message) {
        GetChatMembersCount getChatMembersCount = new GetChatMembersCount();
        getChatMembersCount.setChatId(message.getChatId());
        try {
            return sendApiMethod(getChatMembersCount) - 1;
        } catch (TelegramApiException e) {
            e.printStackTrace();
            return 0;
        }
    }

    private void newGame(Message message) {
        if (getCountMembers(message) < 2) {
            sendMsg(message, "В чате маловато народу, позови еще кого-нибудь");
            return;
        }
        Game newGame = new Game(message.getChat());
        if (findGameById(newGame.getChat().getId()) != null) {
            sendMsg(message, "В этом чате уже есть игра");
        } else {
            games.add(newGame);
            sendMsg(message, "Новая игра успешно создана, для того чтобы присоединиться," +
                    " нужно открыть личный чат с ботом и отправить ему команду\n <code>/enterGame "
                    + newGame.getChat().getId() + "</code>\n Чтобы закрыть набор игроков и приступить" +
                    " к игре, воспользуйтесь командой \n/closeInvite ");
        }
    }

    @Override
    public void processNonCommandUpdate(Update update) {

    }

    public String getBotToken() {
        return "1042713529:AAED4qAkvX16PLynfTJ8K2tmiYwbU8-VKDY";
    }
}
