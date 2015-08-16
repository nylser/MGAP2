package net.mineguild.MGAP2.commands;

import com.google.common.base.Optional;
import net.mineguild.MGAP2.MGAP2;
import org.spongepowered.api.Game;
import org.spongepowered.api.data.key.Keys;
import org.spongepowered.api.data.manipulator.mutable.item.PagedData;
import org.spongepowered.api.entity.EntityTypes;
import org.spongepowered.api.entity.Item;
import org.spongepowered.api.entity.player.Player;
import org.spongepowered.api.item.ItemTypes;
import org.spongepowered.api.item.inventory.ItemStack;
import org.spongepowered.api.text.Text;
import org.spongepowered.api.text.Texts;
import org.spongepowered.api.text.format.TextColors;
import org.spongepowered.api.util.TextMessageException;
import org.spongepowered.api.util.command.CommandException;
import org.spongepowered.api.util.command.CommandResult;
import org.spongepowered.api.util.command.CommandSource;
import org.spongepowered.api.util.command.args.CommandContext;
import org.spongepowered.api.util.command.spec.CommandExecutor;

public class LoginMessage {

    private MGAP2 plugin;
    private Game game;

    //private DataManipulator<PagedData> currentManipulator;
    private ItemStack currentBook;

    public LoginMessage(MGAP2 plugin, Game game) {
        this.plugin = plugin;
        this.game = game;
    }

    public ShowMessage showMessage() {
        return new ShowMessage();
    }

    public SetMessage setMessage() {
        return new SetMessage();
    }

    private class ShowMessage implements CommandExecutor {

        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            try {
                src.sendMessage(Texts.of("Login Message: \n", Texts.legacy('&').from(plugin.loginMessage.getString())));
            } catch (TextMessageException e) {
                return CommandResult.empty();
            }
            return CommandResult.success();
        }

    }

    private class SetMessage implements CommandExecutor {

        public CommandResult execute(CommandSource src, CommandContext args) throws CommandException {
            Optional<String> newMessage = args.getOne("message");
            if (newMessage.isPresent()) {
                Text newText;
                try {
                    newText = Texts.json().from(newMessage.get());
                } catch (TextMessageException e) {
                    try {
                        newText = Texts.legacy('&').from(newMessage.get());
                    } catch (TextMessageException e1) {
                        newText = Texts.of(newMessage.get());
                    }
                }
                plugin.loginMessage.setValue(Texts.legacy('&').to(newText));
                src.sendMessage(Texts.of("Login Message changed to: '", newText, "'"));
                return CommandResult.success();
            } else {
                src.sendMessage(Texts.of(TextColors.RED, "Changing with book not implemented yet."));
                try {
                    ItemStack book = game.getRegistry().createItemBuilder().itemType(ItemTypes.WRITABLE_BOOK).quantity(1).build();
                    PagedData data = book.getOrCreate(PagedData.class).or(plugin.getGame().getRegistry().createBuilderOfType(PagedData.class).get());
                    data.pages().add(Texts.of(plugin.loginMessage.getString()));
                    book.offer(data);
                    book.offer(Keys.DISPLAY_NAME, Texts.of("Login message"));
                    Player p = (Player) src;
                    Item item = (Item) p.getWorld().createEntity(EntityTypes.DROPPED_ITEM, p.getLocation().getPosition()).get();
                    item.offer(item.getItemData().item().set(book));
                    //p.getWorld().spawnEntity(item);
                    p.getInventory().offer(book);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                return CommandResult.empty();
                /*
                try {
                    ItemStack book = game.getRegistry().getItemBuilder().itemType(ItemTypes.WRITABLE_BOOK).quantity(1).
                            itemData(getMessageDataManipulator()).itemData(getDisplayName()).build();
                    if (src instanceof Player) {
                        Player p = (Player) src;
                        if (currentBook == null) {
                            if(p.getInventory().offer(book)) {
                                currentBook = book;
                            }
                        } else if(p.getInventory().contains(currentBook)) {
                            CarriedInventory<? extends Carrier> inventory = p.getInventory();
                            inventory.query(currentBook).clear();
                            Optional<PagedData> data = currentBook.getData(PagedData.class);
                            if(data.isPresent()){
                                PagedData pagedData = data.get();
                                List<Text> texts = pagedData.getAll();
                                TextBuilder combined = Texts.builder();
                                for(Text t : texts){
                                    try {
                                        combined.append(Texts.json().from(t.toString()));
                                    } catch (TextMessageException e){
                                        combined.append(t);
                                    }
                                }
                                src.sendMessage(Texts.of("New Message\n:", combined.build()));
                            }
                            currentBook = null;


                        } else {
                            src.sendMessage(Texts.of("The editing book is already out there."));
                        }
                    }
                } catch (TextMessageException e) {
                    e.printStackTrace();
                }*/
            }
        }

    }

}
