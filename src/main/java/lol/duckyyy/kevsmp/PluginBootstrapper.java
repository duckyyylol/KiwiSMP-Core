package lol.duckyyy.kevsmp;

import io.papermc.paper.plugin.bootstrap.BootstrapContext;
import io.papermc.paper.plugin.bootstrap.PluginBootstrap;
import io.papermc.paper.registry.data.dialog.ActionButton;
import io.papermc.paper.registry.data.dialog.DialogBase;
import io.papermc.paper.registry.data.dialog.action.DialogAction;
import io.papermc.paper.registry.data.dialog.body.DialogBody;
import io.papermc.paper.registry.data.dialog.type.DialogType;
import io.papermc.paper.registry.event.RegistryEvents;
import io.papermc.paper.registry.keys.DialogKeys;
import net.kyori.adventure.key.Key;
import net.kyori.adventure.text.Component;
import net.kyori.adventure.text.event.ClickEvent;
import net.kyori.adventure.text.format.NamedTextColor;
import net.kyori.adventure.text.format.TextDecoration;

import java.util.List;

public class PluginBootstrapper implements PluginBootstrap {
    @Override
    public void bootstrap(BootstrapContext context) {
        DialogAction.StaticAction confirmAction = DialogAction.staticAction(ClickEvent.clickEvent(ClickEvent.Action.RUN_COMMAND, "discord unlink"));

        DialogAction.CustomClickAction cancelAction = DialogAction.customClick(Key.key("kiwismp:unlink/cancel"), null);

        ActionButton confirmButton = ActionButton.builder(Component.text("Unlink").color(NamedTextColor.RED)).tooltip(Component.text("Click to unlink your account")).action(confirmAction).build();

        ActionButton cancelButton = ActionButton.builder(Component.text("Cancel")).tooltip(Component.text("Close this dialog and continue playing")).action(cancelAction).build();

        context.getLifecycleManager().registerEventHandler(RegistryEvents.DIALOG.compose().newHandler(ev -> ev.registry().register(DialogKeys.create(Key.key("kiwismp:unlink_confirm")), builder -> {
            builder.base(DialogBase.builder(Component.text("Confirm Unlinking")).canCloseWithEscape(false).body(List.of(DialogBody.plainMessage(Component.empty().append(Component.text("If you " +
                    "unlink your account, you will: ").color(NamedTextColor.RED)).appendNewline().appendNewline().append(Component.text(KevSmp.config.getBoolean("access_control" +
                    ".link_required_to_join") ? "1. Be kicked from the server" : "1. Be teleported to world spawn").appendNewline().appendNewline().append(Component.text(KevSmp.config.getBoolean(
                            "access_control.link_required_to_join") ? "2. Be required to link your account before rejoining" : "2. Be unable to move until you link a new account"))).appendNewline().appendNewline().append(Component.text("3. Lose any Discord-based perks, such as Twitch subscriber rank.")).appendNewline().appendNewline().appendNewline().append(Component.text("You will " + "NOT:").decorate(TextDecoration.BOLD).color(NamedTextColor.GREEN)).appendNewline().appendNewline().append(Component.text("1. " + "Lose any game progress, like advancements.").appendNewline().appendNewline().append(Component.text("2. Lose any items. Your inventory will remain exactly the same."))).appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().appendNewline().append(Component.text("Unlink Discord account?")).appendNewline().appendNewline().append(Component.text("You can link another Discord account at any time.").color(NamedTextColor.GRAY).decorate(TextDecoration.ITALIC))))).build()).type(DialogType.confirmation(confirmButton, cancelButton));
                }
        )));
    }
}
