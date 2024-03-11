package me.dave.activityrewarder.commands;

import me.dave.activityrewarder.ActivityRewarder;
import me.dave.activityrewarder.data.RewardUser;
import me.dave.activityrewarder.importer.ConfigImporter;
import me.dave.activityrewarder.importer.DailyRewardsPlusImporter;
import me.dave.activityrewarder.importer.NDailyRewardsImporter;
import me.dave.activityrewarder.module.RewardModule;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsGui;
import me.dave.activityrewarder.module.dailyrewards.DailyRewardsModule;
import me.dave.platyutils.command.Command;
import me.dave.platyutils.command.SubCommand;
import me.dave.platyutils.libraries.chatcolor.ChatColorHandler;
import me.dave.platyutils.utils.Updater;
import org.bukkit.Bukkit;
import org.bukkit.command.CommandSender;
import org.bukkit.entity.Player;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

import java.io.FileNotFoundException;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

public class RewardsCommand extends Command {

    public RewardsCommand() {
        super("rewards");
        addSubCommand(new AboutSubCommand());
        addSubCommand(new ClaimSubCommand());
        addSubCommand(new ImportSubCommand());
        addSubCommand(new MessagesSubCommand());
        addSubCommand(new ReloadSubCommand());
        addSubCommand(new ResetSubCommand());
        addSubCommand(new ResetDaysSubCommand());
        addSubCommand(new SetDaysSubCommand());
        addSubCommand(new ResetStreakSubCommand());
        addSubCommand(new SetStreakSubCommand());
        addSubCommand(new UpdateSubCommand());
        addSubCommand(new VersionSubCommand());
    }

    @Override
    public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
        ActivityRewarder.getInstance().getConfigManager().checkRefresh();

        if (!(sender instanceof Player player)) {
            ChatColorHandler.sendMessage(sender, "Console cannot run this command!");
            return true;
        }

        if (!player.hasPermission("activityrewarder.use")) {
            ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("no-permissions"));
            return true;
        }

        ActivityRewarder.getInstance().getModules().stream()
            .filter(module -> module instanceof DailyRewardsModule)
            .findFirst()
            .ifPresentOrElse(
                module -> new DailyRewardsGui((DailyRewardsModule) module, player).open(),
                () -> ChatColorHandler.sendMessage(player, "&#ff6969DailyRewards module is disabled")
            );

        return true;
    }

    // TODO: Test
    private static class AboutSubCommand extends SubCommand {
        private static final String[] ABOUT_MESSAGES = new String[]{
            "&#A5B8FE&lLushRewards &#C4B6FE(v" + ActivityRewarder.getInstance().getDescription().getVersion() + ")",
            "&7An extremely configurable, feature rich rewards plugin. ",
            "&7Reward your players each day for logging in and also reward ",
            "&7them for their time spent on the server with playtime rewards!",
            "&r",
            "&7Author: &#f7ba6fDav_e_",
            "&r",
            "&7Wiki: &#f7ba6fhttps://dave-12.gitbook.io/lush-rewards",
            "&7Support: &#f7ba6fhttps://discord.gg/p3duRZsZ2f"
        };

        public AboutSubCommand() {
            super("about");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            for (String aboutMessage : ABOUT_MESSAGES) {
                ChatColorHandler.sendMessage(sender, aboutMessage);
            }
            return true;
        }
    }

    // TODO: Test
    private static class ClaimSubCommand extends SubCommand {

        public ClaimSubCommand() {
            super("claim");
            addRequiredPermission("activityrewarder.use");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (!(sender instanceof Player player)) {
                ChatColorHandler.sendMessage(sender, "Console cannot run this command!");
                return true;
            }

            AtomicInteger rewardsGiven = new AtomicInteger();
            ActivityRewarder.getInstance().getModules().forEach(module -> {
                if (module instanceof RewardModule rewardModule) {
                    rewardModule.claimRewards(player);
                    rewardsGiven.getAndIncrement();
                }
            });


            if (rewardsGiven.get() == 0) {
                ChatColorHandler.sendMessage(player, ActivityRewarder.getInstance().getConfigManager().getMessage("no-rewards-available"));
            }

            return true;
        }
    }

    // TODO: Test
    private static class ImportSubCommand extends SubCommand {

        public ImportSubCommand() {
            super("import");
            addRequiredPermission("activityrewarder.import");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 0) {
                ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards import <plugin>"));
            }

            ConfigImporter configImporter = null;
            try {
                switch (args[1].toLowerCase()) {
                    case "dailyrewardsplus" -> configImporter = new DailyRewardsPlusImporter();
                    case "ndailyrewards" -> configImporter = new NDailyRewardsImporter();
                }
            } catch (FileNotFoundException e) {
                ChatColorHandler.sendMessage(sender, "&#ff6969Could not find files when attempting to import from &#d13636'" + args[1] + "'");
                return true;
            }

            if (configImporter != null) {
                long startMs = Instant.now().toEpochMilli();
                configImporter.startImport()
                    .completeOnTimeout(false, 10, TimeUnit.SECONDS)
                    .thenAccept(success -> {
                        if (success) {
                            ChatColorHandler.sendMessage(sender, "&#b7faa2Successfully imported configuration from &#66b04f'" + args[1] + "' &#b7faa2in &#66b04f" + (Instant.now().toEpochMilli() - startMs) + "ms");
                            ActivityRewarder.getInstance().getConfigManager().reloadConfig();
                            ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("reload"));
                        } else {
                            ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + args[1] + "' &#ff6969in &#d13636" + (Instant.now().toEpochMilli() - startMs) + "ms");
                        }
                    });
            } else {
                ChatColorHandler.sendMessage(sender, "&#ff6969Failed to import configuration from &#d13636'" + args[1] + "'");
            }

            return true;
        }

        @Override
        public @Nullable List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            return List.of("DailyRewardsPlus");
        }
    }

    // TODO: Test
    private static class MessagesSubCommand extends SubCommand {

        public MessagesSubCommand() {
            super("messages");
            addRequiredPermission("activityrewarder.viewmessages");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            ActivityRewarder.getInstance().getConfigManager().getMessages().forEach(message -> ChatColorHandler.sendMessage(sender, message));
            return true;
        }
    }

    // TODO: Test
    private static class ReloadSubCommand extends SubCommand {

        public ReloadSubCommand() {
            super("reload");
            addRequiredPermission("activityrewarder.reload");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            ActivityRewarder.getInstance().getConfigManager().reloadConfig();
            ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("reload"));
            return true;
        }
    }

    // TODO: Test
    private static class ResetSubCommand extends SubCommand {

        public ResetSubCommand() {
            super("reset");
            addRequiredPermission("activityrewarder.reset");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            switch (args.length) {
                case 0 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset <player>"));
                case 1 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("reset").replaceAll("%target%", args[0]));
                case 2 -> {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset <player> confirm"));
                        return true;
                    }

                    if (!setDay(sender, args[0], 1) || !setStreak(sender, args[0], 1) || !removeCollectedDays(sender, args[0])) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-days-confirm").replaceAll("%target%", args[0]).replaceAll("%day%", "1"));
                }
            }

            return true;
        }

        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else {
                return null;
            }
        }
    }

    // TODO: Test
    private static class ResetDaysSubCommand extends SubCommand {

        public ResetDaysSubCommand() {
            super("reset-days");
            addRequiredPermission("activityrewarder.resetdays");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            switch (args.length) {
                case 0 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset-days <player>"));
                case 1 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("reset-days").replaceAll("%target%", args[0]));
                case 2 -> {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset-days <player> confirm"));
                        return true;
                    }

                    if (!setDay(sender, args[0], 1)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-days-confirm").replaceAll("%target%", args[0]).replaceAll("%day%", "1"));
                    return true;
                }
            }

            return true;
        }

        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else {
                return null;
            }
        }
    }

    // TODO: Test
    private static class SetDaysSubCommand extends SubCommand {

        public SetDaysSubCommand() {
            super("set-days");
            addRequiredPermission("activityrewarder.setdays");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            switch (args.length) {
                case 0, 1 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-days <player> <day-num>"));
                case 2 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-days").replaceAll("%target%", args[0]).replaceAll("%day%", args[1]));
                case 3 -> {
                    if (!args[2].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-days <player> <day-num> confirm"));
                        return true;
                    }

                    int dayNum;
                    try {
                        dayNum = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-days <player> <day-num> confirm"));
                        return true;
                    }

                    if (!setDay(sender, args[0], dayNum)) return true;
                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-days-confirm").replaceAll("%target%", args[0]).replaceAll("%day%", String.valueOf(dayNum)));
                    return true;
                }
            }

            return true;
        }

        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else {
                return null;
            }
        }
    }

    // TODO: Test
    private static class ResetStreakSubCommand extends SubCommand {

        public ResetStreakSubCommand() {
            super("reset-streak");
            addRequiredPermission("activityrewarder.resetstreak");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            switch (args.length) {
                case 0 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset-streak <player>"));
                case 1 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("reset-streak").replaceAll("%target%", args[0]));
                case 2 -> {
                    if (!args[1].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards reset-streak <player> confirm"));
                        return true;
                    }

                    if (!setStreak(sender, args[0], 1)) {
                        return true;
                    }

                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-streak-confirm").replaceAll("%target%", args[0]).replaceAll("%streak%", "1"));
                    return true;
                }
            }
            return true;
        }

        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else {
                return null;
            }
        }
    }

    // TODO: Test
    private static class SetStreakSubCommand extends SubCommand {

        public SetStreakSubCommand() {
            super("set-streak");
            addRequiredPermission("activityrewarder.setstreak");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            switch (args.length) {
                case 0, 1 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak>"));
                case 2 -> ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-streak").replaceAll("%target%", args[0]).replaceAll("%streak%", args[1]));
                case 3 -> {
                    if (!args[2].equalsIgnoreCase("confirm")) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak> confirm"));
                        return true;
                    }

                    int streak;
                    try {
                        streak = Integer.parseInt(args[1]);
                    } catch (NumberFormatException e) {
                        ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("incorrect-usage").replaceAll("%command-usage%", "/rewards set-streak <player> <streak> confirm"));
                        return true;
                    }

                    if (!setStreak(sender, args[0], streak)) return true;
                    ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("set-streak-confirm").replaceAll("%target%", args[0]).replaceAll("%streak%", String.valueOf(streak)));
                    return true;
                }
            }

            return true;
        }

        @Override
        public List<String> tabComplete(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            if (args.length == 1) {
                return Bukkit.getOnlinePlayers().stream().map(Player::getName).toList();
            } else {
                return null;
            }
        }
    }

    // TODO: Test
    private static class UpdateSubCommand extends SubCommand {

        public UpdateSubCommand() {
            super("update");
            addRequiredPermission("activityrewarder.update");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            Updater updater = ActivityRewarder.getInstance().getUpdater();

            if (updater.isAlreadyDownloaded() || !updater.isUpdateAvailable()) {
                ChatColorHandler.sendMessage(sender, "&#ff6969It looks like there is no new update available!");
                return true;
            }

            updater.downloadUpdate().thenAccept(success -> {
                if (success) {
                    ChatColorHandler.sendMessage(sender, "&#b7faa2Successfully updated LushRewards, restart the server to apply changes!");
                } else {
                    ChatColorHandler.sendMessage(sender, "&#ff6969Failed to update LushRewards!");
                }
            });

            return true;
        }
    }

    // TODO: Test
    private static class VersionSubCommand extends SubCommand {

        public VersionSubCommand() {
            super("version");
            addRequiredPermission("activityrewarder.version");
        }

        @Override
        public boolean execute(@NotNull CommandSender sender, @NotNull org.bukkit.command.Command command, @NotNull String label, @NotNull String[] args) {
            ChatColorHandler.sendMessage(sender, "&#a8e1ffYou are currently running LushRewards version &#58b1e0" + ActivityRewarder.getInstance().getDescription().getVersion());
            return true;
        }
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean setDay(CommandSender sender, String nameOrUuid, int dayNum) {
        Player player = Bukkit.getPlayer(nameOrUuid);
        UUID uuid;
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            try {
                uuid = UUID.fromString(nameOrUuid);
            } catch (IllegalArgumentException e) {
                ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("unknown-player").replaceAll("%player%", nameOrUuid));
                return false;
            }
        }

        if (player != null && ActivityRewarder.getInstance().getDataManager().isRewardUserLoaded(uuid)) {
            RewardUser rewardUser = ActivityRewarder.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData) {
                moduleUserData.setDayNum(dayNum);
                moduleUserData.setLastCollectedDate(LocalDate.of(1971, 10, 1)); // The date Walt Disney World was opened
                rewardUser.save();
            }
        } else {
            ActivityRewarder.getInstance().getDataManager().loadRewardUser(uuid).thenAccept((rewardUser -> {
                if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData) {
                    moduleUserData.setDayNum(dayNum);
                    moduleUserData.setLastCollectedDate(LocalDate.of(1971, 10, 1)); // The date Walt Disney World was opened
                    rewardUser.save();
                }
                ActivityRewarder.getInstance().getDataManager().unloadRewarderUser(uuid);
            }));
        }

        return true;
    }

    @SuppressWarnings("BooleanMethodIsAlwaysInverted")
    private static boolean setStreak(CommandSender sender, String nameOrUuid, int streak) {
        Player player = Bukkit.getPlayer(nameOrUuid);
        UUID uuid;
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            try {
                uuid = UUID.fromString(nameOrUuid);
            } catch (IllegalArgumentException e) {
                ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("unknown-player").replaceAll("%player%", nameOrUuid));
                return false;
            }
        }

        if (player != null && ActivityRewarder.getInstance().getDataManager().isRewardUserLoaded(uuid)) {
            RewardUser rewardUser = ActivityRewarder.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData) {
                moduleUserData.setStreakLength(streak);
                rewardUser.save();
            }
        } else {
            ActivityRewarder.getInstance().getDataManager().loadRewardUser(uuid).thenAccept((rewardUser -> {
                if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData) {
                    moduleUserData.setStreakLength(streak);
                    rewardUser.save();
                }
                ActivityRewarder.getInstance().getDataManager().unloadRewarderUser(uuid);
            }));
        }

        return true;
    }

    private static boolean removeCollectedDays(CommandSender sender, String nameOrUuid) {
        Player player = Bukkit.getPlayer(nameOrUuid);
        UUID uuid;
        if (player != null) {
            uuid = player.getUniqueId();
        } else {
            try {
                uuid = UUID.fromString(nameOrUuid);
            } catch (IllegalArgumentException e) {
                ChatColorHandler.sendMessage(sender, ActivityRewarder.getInstance().getConfigManager().getMessage("unknown-player").replaceAll("%player%", nameOrUuid));
                return false;
            }
        }

        if (player != null && ActivityRewarder.getInstance().getDataManager().isRewardUserLoaded(uuid)) {
            RewardUser rewardUser = ActivityRewarder.getInstance().getDataManager().getRewardUser(player);
            if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData) {
                moduleUserData.clearCollectedDates();
                moduleUserData.setLastCollectedDate(null);
                rewardUser.save();
            }
        } else {
            ActivityRewarder.getInstance().getDataManager().loadRewardUser(uuid).thenAccept((rewardUser -> {
                if (rewardUser.getModuleData(DailyRewardsModule.ID) instanceof DailyRewardsModule.UserData moduleUserData) {
                    moduleUserData.clearCollectedDates();
                    moduleUserData.setLastCollectedDate(null);
                    rewardUser.save();
                }
                ActivityRewarder.getInstance().getDataManager().unloadRewarderUser(uuid);
            }));
        }

        return true;
    }
}
