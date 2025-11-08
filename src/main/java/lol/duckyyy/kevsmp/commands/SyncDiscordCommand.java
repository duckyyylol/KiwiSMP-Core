package lol.duckyyy.kevsmp.commands;

import github.scarsz.discordsrv.DiscordSRV;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Guild;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Member;
import github.scarsz.discordsrv.dependencies.jda.api.entities.Role;
import github.scarsz.discordsrv.dependencies.jda.api.entities.User;
import lol.duckyyy.kevsmp.KevSmp;
import net.kyori.adventure.text.Component;
import net.luckperms.api.node.Node;
import org.bukkit.command.Command;
import org.bukkit.command.CommandExecutor;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;

public class SyncDiscordCommand implements CommandExecutor {
    @Override
    public boolean onCommand(@NotNull CommandSender sender, @NotNull Command command, @NotNull String label, @NotNull String @NotNull [] args) {
        if(!(sender instanceof Player player)) {
            sender.sendMessage(Component.text("This command can only be run by a user."));
            return true;
        }
        boolean linked = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId()) != null;

        if(!linked) {
            player.sendMessage(KevSmp.componentWithPrefix(Component.text("You must link your account before using this command.")));

            return true;
        }

        String linkedUserId = DiscordSRV.getPlugin().getAccountLinkManager().getDiscordId(player.getUniqueId());

        if(linkedUserId == null) {
            player.sendMessage(KevSmp.componentWithPrefix(Component.text("You must link your account before using this command.")));

            return true;
        }

        User linkedUser = DiscordSRV.getPlugin().getJda().getUserById(linkedUserId);
        if(linkedUser == null) {
            player.sendMessage(KevSmp.componentWithPrefix(Component.text("Something went wrong when syncing. Please try again.")));

            return true;
        }

        Guild guild = linkedUser.getJDA().getGuildById(KevSmp.config.getString("discord.guild_id"));
        Member guildMember = guild.getMemberById(linkedUser.getId());
        Role subscriberRole = guild.getRoleById(KevSmp.config.getString("discord.roles.subscriber_role_id"));

        String status = "default";


        if(guildMember.getRoles().contains(subscriberRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
            // no action required (message)
            status = "subscriber";
            player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("No action required. Status of %s unchanged", status))));
            return true;
        } else if(!guildMember.getRoles().contains(subscriberRole) && KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
            // remove from group (status lost)
            KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                u.data().remove(Node.builder("group.subscriber").build());
                u.data().add(Node.builder("group.default").build());
            });
            player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("Sync completed. Now %s status.", status))));
            return true;
        } else if(guildMember.getRoles().contains(subscriberRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())) {
            // add to group (status gained)
            KevSmp.luckpermsApi.getUserManager().modifyUser(player.getUniqueId(), u -> {
                u.data().add(Node.builder("group.subscriber").build());
            });
            status = "subscriber";
            player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("Sync completed. Now %s status.", status))));

            return true;
        } else if(!guildMember.getRoles().contains(subscriberRole) && !KevSmp.luckpermsApi.getUserManager().getUser(player.getUniqueId()).getNodes().contains(Node.builder("group.subscriber").build())){
            player.sendMessage(KevSmp.componentWithPrefix(Component.text(String.format("No action required. Status of %s unchanged", status))));
            return true;
        } else {
            player.sendMessage(KevSmp.componentWithPrefix(Component.text("Something went wrong when syncing. Please try again.")));

            return true;
        }
    }
}
