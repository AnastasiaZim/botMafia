import org.telegram.telegrambots.meta.api.objects.Chat;
import org.telegram.telegrambots.meta.api.objects.User;

import java.util.ArrayList;
import java.util.List;

public class Game {
    private enum STATUS {
        INVITE_PLAYERS,
        INVITE_END,
        MAFIA_STEP,
        CITIZEN_STEP
    }

    public void closeInvite() {
        status = STATUS.INVITE_END;
    }

    public void setMafiaStep() {
        status = STATUS.MAFIA_STEP;
    }

    public void setCitizenStep() {
        status = STATUS.CITIZEN_STEP;
    }

    private STATUS status;
    private Chat chat;
    private List<Player> playersList = new ArrayList<>();

    public Game(Chat chat) {
        status = STATUS.INVITE_PLAYERS;
        this.chat = chat;
    }

    public int votesCount = 0;

    public int countPlayers() {
        return playersList.size();
    }

    public boolean addPlayer(User user, Long chatId) {
        if (status != STATUS.INVITE_PLAYERS) {
            return false;
        }
        for (Player player : playersList) {
            if (player.getUser().getId().equals(user.getId())) {
                return false;
            }
        }
        Player newUser = new Player(chatId, user);
        playersList.add(newUser);
        return true;
    }

    public Chat getChat() {
        return chat;
    }

    public List<Player> initRoles() {
        if (status == STATUS.INVITE_END) {
            for (Player player : playersList) {
                player.setRole(Player.ROLE.CITIZEN);
            }
            int rand = (int) (Math.random() * (countPlayers()));
            playersList.get(rand).setRole(Player.ROLE.MAFIA);
            return playersList;
        } else {
            return null;
        }
    }

    public Player getMafia() {
        for (Player player : playersList) {
            if (player.getRole() == Player.ROLE.MAFIA) {
                return player;
            }
        }
        return null;
    }

    public Player kill(String name) {
        if (status == STATUS.MAFIA_STEP)
            for (Player player : playersList) {
                if (player.getUser().getFirstName().equals(name) && player.getRole() != Player.ROLE.MAFIA) {
                    playersList.remove(player);
                    return player;
                }
            }
        return null;
    }

    public void setPlayerToPrison(Player player) {
        playersList.remove(player);
    }

    public Player getPlayerByName(String name) {
        for (Player player : playersList) {
            if (player.getUser().getFirstName().equals(name)) {
                return player;
            }
        }
        return null;
    }

    public Player getPlayerWithMaxCountVotes() {
        Player votedPlayer = null;
        int max = 0;
        for (Player player : playersList) {
            if (player.votes > max) {
                max = player.votes;
                votedPlayer = player;
            } else if (player.votes == max) {
                votedPlayer = null;
            }
        }
        return votedPlayer;
    }
}
