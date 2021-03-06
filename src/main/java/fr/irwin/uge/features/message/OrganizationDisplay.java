package fr.irwin.uge.features.message;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;
import fr.irwin.uge.UGEBot;
import fr.irwin.uge.features.MessageFeature;
import fr.irwin.uge.internals.EventWaiter;
import fr.irwin.uge.utils.DateUtils;
import fr.irwin.uge.utils.RedisUtils;
import fr.irwin.uge.utils.RolesUtils;
import net.dv8tion.jda.api.EmbedBuilder;
import net.dv8tion.jda.api.entities.Guild;
import net.dv8tion.jda.api.entities.Message;
import net.dv8tion.jda.api.entities.MessageEmbed;
import net.dv8tion.jda.api.entities.TextChannel;
import net.dv8tion.jda.api.events.message.guild.react.GuildMessageReactionAddEvent;

import java.util.Calendar;
import java.util.Date;
import java.util.Locale;
import java.util.Map;

public class OrganizationDisplay extends MessageFeature
{
    private final String title;
    private final Map<String, Field> fields;

    private long messageId;
    private Date start;
    private Date end;

    public OrganizationDisplay(long guildId, long textChannelId, String title, Map<String, Field> fields) {
        super(guildId, textChannelId);
        this.title = title;
        this.fields = fields;
        setDates();
    }

    /* Don't mind this atrocity, only for deserialization purpose. */
    @JsonCreator
    public OrganizationDisplay(
            @JsonProperty("guildId") long guildId,
            @JsonProperty("textChannelId") long textChannelId,
            @JsonProperty("title") String title,
            @JsonProperty("fields") Map<String, Field> fields,
            @JsonProperty("messageId") long messageId, @JsonProperty("start") Date start, @JsonProperty("end") Date end) {
        super(guildId, textChannelId);
        this.title = title;
        this.fields = fields;
        this.messageId = messageId;
        this.start = start;
        this.end = end;
    }

    @Override
    public void send() {
        final MessageEmbed messageEmbed = getEmbed();

        final Guild guild = UGEBot.JDA().getGuildById(guildId);
        if (guild == null) {
            return;
        }

        final TextChannel textChannel = guild.getTextChannelById(textChannelId);
        if (textChannel == null) {
            return;
        }

        Message message = textChannel.sendMessage(messageEmbed).complete();
        start(message);
        RedisUtils.addFeature(guild, messageId, this);
    }

    @Override
    protected void start(Message m) {
        messageId = m.getIdLong();
        fields.keySet().forEach(emote -> m.addReaction(emote.replace(">", "")).queue());
        new EventWaiter.Builder(GuildMessageReactionAddEvent.class, e -> e.getMessageIdLong() == messageId &&
                RolesUtils.isTeacher(e.getMember()), (e, ew) -> {
            String emote;
            try {
                emote = e.getReactionEmote().getEmoji();
            } catch (IllegalStateException ex) {
                emote = "<:" + e.getReactionEmote().getAsReactionCode() + '>';
            }

            if (fields.containsKey(emote)) {
                fields.get(emote).cycleState();
                update();
                e.getReaction().removeReaction(e.getUser()).queue();
            } else if (emote.equals("???")) {
                ew.close();
                RedisUtils.removeFeature(e.getGuild(), messageId, this);
            } else if (emote.equals("????")) {
                update();
                e.getReaction().removeReaction(e.getUser()).queue();
            }
        }).autoClose(false).build();
    }

    private void update() {
        setDates();
        final MessageEmbed messageEmbed = getEmbed();

        final Guild guild = UGEBot.JDA().getGuildById(guildId);
        if (guild == null) {
            return;
        }

        final TextChannel textChannel = guild.getTextChannelById(textChannelId);
        if (textChannel == null) {
            return;
        }

        final Message message = textChannel.retrieveMessageById(messageId).complete();
        if (message == null) {
            return;
        }
        message.editMessage(messageEmbed).queue();

        /* Updating Redis */
        RedisUtils.addFeature(guild, messageId, this);
    }

    private MessageEmbed getEmbed() {

        EmbedBuilder builder = new EmbedBuilder().setTitle(title).setDescription(String.format("Semaine du **%s** au **%s**", DateUtils.formatDate(start), DateUtils.formatDate(end)));

        for (Map.Entry<String, Field> entry : fields.entrySet()) {
            Field field = entry.getValue();
            builder.addField(field.name, field.state.value, field.inline);
        }

        return builder.build();
    }

    private void setDates() {
        Calendar calendar = Calendar.getInstance(Locale.FRANCE);
        calendar.set(Calendar.DAY_OF_WEEK, 2);
        this.start = calendar.getTime();
        calendar.set(Calendar.DAY_OF_WEEK, 1);
        this.end = calendar.getTime();
    }

    public enum State
    {
        UNKNOWN("*Information ?? venir*"), ON_SITE("Pr??sentiel"), HYBRID("Hybride"), REMOTE("?? distance");

        private final String value;

        State(String value) {
            this.value = value;
        }
    }

    public static class Field
    {

        private final String name;
        private final boolean inline;
        private State state;

        @JsonCreator
        public Field(@JsonProperty("name") String name, @JsonProperty("inline") boolean inline) {
            this.name = name;
            this.inline = inline;
            this.state = State.UNKNOWN;
        }

        private void cycleState() {
            state = State.values()[(state.ordinal() + 1) % State.values().length];
        }
    }
}
