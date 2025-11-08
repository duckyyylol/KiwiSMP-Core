package lol.duckyyy.kevsmp.events;

import github.scarsz.discordsrv.api.Subscribe;
import github.scarsz.discordsrv.api.events.AccountLinkedEvent;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.listeners.DiscordAccountLinkListener;
import io.papermc.paper.dialog.Dialog;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.TextComponent;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;
import net.kyori.adventure.text.minimessage.MiniMessage;
import net.luckperms.api.node.Node;
import org.bukkit.Bukkit;
import org.bukkit.entity.Player;

import java.util.List;

public class AccountLinkListener extends DiscordAccountLinkListener {
    @Subscribe
    public void onAccountLinked(AccountLinkedEvent ev) {
        Player player = Bukkit.getPlayer(ev.getPlayer().getUniqueId());
        assert player != null;
        if (player.isOnline()) {

            DialogAction.StaticAction confirmAction = DialogAction.staticAction(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "discord unlink"));

            DialogAction.CustomClickAction cancelAction = DialogAction.customClick(Key.key("kiwismp:unlink/cancel"), null);

            ActionButton confirmButton = ActionButton.builder(Component.text("Unlink").color(NamedTextColor.RED)).tooltip(Component.text("Click to unlink your account")).action(confirmAction).build();

            ActionButton cancelButton = ActionButton.builder(Component.text("Cancel")).tooltip(Component.text("Close this dialog and continue playing")).action(cancelAction).build();

            Dialog unlinkConfirmDialog =
                    Dialog.create(d -> d.empty().base(DialogBase.builder(Component.text("Confirm Unlinking")).canCloseWithEscape(false).body(List.of(DialogBody.plainMessage(Component.empty().append(Component.text("If you unlink your account, you will: ").color(NamedTextColor.RED)).appendNewline().appendNewline().append(Component.text(KevSmp.config.getBoolean("access_control.link_required_to_join") ? "1. Be kicked from the server" : "1. Be teleported to world spawn").appendNewline().appendNewline().append(Component.text(KevSmp.config.getBoolean("access_control.link_required_to_join") ? "2. Be required to link your account before rejoining" : "2. Be unable to move until you link a new account"))).appendNewline().appendNewline().append(Component.text("3. Lose any Discord-based perks, such as Twitch subscriber rank.")).appendNewline().appendNewline().appendNewline().append(Component.text("You will " + "NOT:").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN)).appendNewline().appendNewline().append(Component.text("1. " + "Lose any game progress, like advancements.").appendNewline().appendNewline().append(Component.text("2. Lose any items. Your inventory will remain exactly the same."))).appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().append(Component.text(String.format("Unlink Minecraft account %s from Discord user @%s (%s)?", ev.getPlayer().getName(), ev.getUser().getName(), ev.getUser().getId()))).appendNewline().appendNewline().append(Component.text("You can link another Discord account at any time.").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))))).build()).type(DialogType.confirmation(confirmButton, cancelButton)));

            Guild guild = ev.getUser().getJDA().getGuildById(KevSmp.config.getString("discord.guild_id"));
            Member guildMember = guild.getMemberById(ev.getUser().getId());
            Role subscriberRole = guild.getRoleById(KevSmp.config.getString("discord.roles.subscriber_role_id"));

            TextComponent responseComponent = KevSmp.componentWithPrefix(Component.text(String.format("Thank you for linking your account! Your Minecraft account (%s) was successfully linked to @%s" +
                    " (%s)", ev.getPlayer().getName(), ev.getUser().getName(), ev.getUser().getId()))).appendNewline().append(Component.text("[Click Here to Unlink your Account]").color(NamedTextColor.GREEN).decorate(TextDecoration.BOLD).clickEvent(ClickEvent.clickEvent(ClickEvent.Action.SHOW_DIALOG, ClickEvent.Payload.dialog(unlinkConfirmDialog)))).appendNewline().appendNewline();

            if (guildMember != null && guildMember.getRoles().contains(subscriberRole)) {

                KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                    u.data().add(Node.builder("group.subscriber").build());
                });
                TextComponent newResponseComponent =
                        responseComponent.append(Component.text("!").color(NamedTextColor.GOLD).decorate(TextDecoration.BOLD)).appendSpace().append(Component.empty()).append(Component.text("Since " + "you " + "are a " + "Twitch subscriber, you earned the").color(NamedTextColor.WHITE).decorate().appendSpace().append(MiniMessage.miniMessage().deserialize("<b" + "><gradient" + ":#E43A96:#D78AB4>ѕᴜʙѕᴄ</gradient><gradient:#D78AB4:#E43A96>ʀɪʙᴇʀ</gradient></b>")).appendSpace().append(Component.text("rank!").color(NamedTextColor.WHITE)));

                player.sendMessage(newResponseComponent);
            } else {
                KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                    if (u.data().toMap().containsKey(Node.builder("group.subscriber")))
                        u.data().remove(Node.builder("group.subscriber").build());
                });
            }


        }
    }
}
