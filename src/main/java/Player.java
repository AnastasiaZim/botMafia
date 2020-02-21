import org.telegram.telegrambots.meta.api.objects.User;

public class Player {
    private Long chatId;
    private User user;
    private ROLE role;

    public int votes = 0;

    public ROLE getRole() {
        return role;
    }

    public void setRole(ROLE role) {
        this.role = role;
    }

    public enum ROLE {
        CITIZEN,
        MAFIA,
    }

    public Player(Long chatId, User user) {
        this.chatId = chatId;
        this.user = user;
    }

    public Long getChatId() {
        return chatId;
    }

    public User getUser() {
        return user;
    }
}
