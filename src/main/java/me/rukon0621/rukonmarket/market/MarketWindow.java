package me.rukon0621.rukonmarket.market;

import com.google.common.io.ByteArrayDataInput;
import com.google.common.io.ByteArrayDataOutput;
import me.rukon0621.callback.LogManager;
import me.rukon0621.callback.ProxyCallBack;
import me.rukon0621.guardians.GUI.MenuWindow;
import me.rukon0621.guardians.data.ItemData;
import me.rukon0621.guardians.data.PlayerData;
import me.rukon0621.guardians.data.TypeData;
import me.rukon0621.guardians.helper.InvClass;
import me.rukon0621.guardians.helper.ItemClass;
import me.rukon0621.guardians.helper.Msg;
import me.rukon0621.guardians.helper.Pair;
import me.rukon0621.guardians.main;
import me.rukon0621.guardians.offlineMessage.OfflineMessageManager;
import me.rukon0621.pay.PaymentData;
import me.rukon0621.pay.RukonPayment;
import me.rukon0621.rukonmarket.RukonMarket;
import me.rukon0621.rukonmarket.listener.ChatListener;
import org.bukkit.Material;
import org.bukkit.Sound;
import org.bukkit.entity.Player;
import org.bukkit.event.EventHandler;
import org.bukkit.event.HandlerList;
import org.bukkit.event.Listener;
import org.bukkit.event.inventory.ClickType;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryCloseEvent;
import org.bukkit.event.player.PlayerQuitEvent;
import org.bukkit.inventory.ItemStack;
import org.bukkit.scheduler.BukkitRunnable;

import java.sql.SQLException;
import java.util.*;
import java.util.concurrent.CountDownLatch;
import java.util.stream.Collectors;

import static me.rukon0621.guardians.main.pfix;


public class MarketWindow implements Listener {

    enum SearchType {
        COST("price"),
        LEVEL("level"),
        TIME("uuid");

        final String dbFieldName;

        SearchType(String dbFieldName) {
            this.dbFieldName = dbFieldName;
        }

    }

    private static final String myItemSpeakerGUITitle = "&f&l\uF000\uF018";
    private static final Set<Player> playerOnWindow = new HashSet<>();
    private static final int basicSlots = 3; //기본 판매 가능 슬롯
    private static final int deadLineDays = 7; //아이템 등록 만료기간
    private static final Set<Player> noClick = new HashSet<>();
    private static final RukonMarket plugin = RukonMarket.inst();
    private static final MarketManager manager = RukonMarket.inst().getMarketManager();
    private static final String mainGUI = "&f\uF000\uF017";
    private static final String myItemGUI = "&f\uF000\uF018";
    private static final String registerGUI = "&f\uF000\uF019";

    private boolean disabled = false;
    private final Player player;
    private SearchType searchType;
    private boolean reversed = false;
    private final List<MarketItem> myItems;
    private final List<MarketItem> searchedList;
    private int page = 1;
    private InvClass inv;
    private boolean trueClose = true; //이게 true면 인벤토리를 닫을시 WINDOW가 삭제됨
    private Pair levelFilter;
    private String nameFilter;
    private String typeFilter;
    private long price;
    private ItemStack selectedItem;
    private int lastPage = 1;
    private long clickCool = 0L;

    public static void resetPlayerUsingMarket(Player player) {
        playerOnWindow.remove(player);
    }

    public static boolean isPlayerUsingMarket(Player player) {
        return playerOnWindow.contains(player);
    }

    /**
     * Open The Market
     *
     * @param player player
     */
    public MarketWindow(Player player) {
        this.player = player;
        playerOnWindow.add(player);
        plugin.getServer().getPluginManager().registerEvents(this, plugin);
        searchType = SearchType.TIME;
        myItems = new ArrayList<>();
        searchedList = new ArrayList<>();
        nameFilter = null;
        levelFilter = null;
        typeFilter = null;
        price = 0;
        selectedItem = null;
        reloadMainMenu();
    }

    private void disable() {
        disabled = true;
        myItems.clear();
        searchedList.clear();
        HandlerList.unregisterAll(this);
        playerOnWindow.remove(player);
        player.closeInventory();
    }

    /**
     * 비동기로 메뉴를 염. latch가 await되는 동안 마켓과 아무 상호작용도 할 수 없음.
     */
    private void reloadMainMenu() {
        reloadMainMenu(false);
    }

    private void reloadMainMenu(boolean last) {
        this.inv = new InvClass(6, mainGUI);
        trueClose = false;
        noClick.add(player);

        new BukkitRunnable() {
            @Override
            public void run() {
                manager.search(searchedList, searchType, reversed, nameFilter, typeFilter, levelFilter, page);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        if (searchedList.isEmpty()) {
                            page = lastPage;
                            if (!last) {
                                reloadMainMenu(true);
                                return;
                            }
                        }
                        changeMainPage();
                        player.openInventory(inv.getInv());
                        noClick.remove(player);
                        trueClose = true;
                    }
                }.runTaskLater(plugin, 2);
            }
        }.runTaskAsynchronously(plugin);

    }

    private void changeMainPage() {
        inv.getInv().clear();
        int slot = 0;
        for (MarketItem item : searchedList) {
            if (slot % 9 == 7) slot += 2;
            inv.setslot(slot, item.getIcon(player.isOp()));
            slot++;
        }

        ItemClass item = new ItemClass(new ItemStack(Material.SCUTE), "&6이름 검색");
        item.setCustomModelData(7);
        item.addLore("&f이름을 입력해 아이템을 검색합니다");
        if (nameFilter != null) {
            item.setName("&6이름 검색 : " + nameFilter);
            item.addLore("&f ");
            item.addLore("&c쉬프트 우클릭&f을 이용해 검색 필터를 제거합니다.");
        }
        inv.setslot(7, item.getItem());
        inv.setslot(8, item.getItem());

        item = new ItemClass(new ItemStack(Material.SCUTE), "&6타입 검색");
        item.setCustomModelData(7);
        item.addLore("&f해당 타입과 그 하위 타입을 검색합니다.");
        if (typeFilter != null) {
            item.setName("&6타입 검색 : " + typeFilter);
            item.addLore("&f ");
            item.addLore("&c쉬프트 우클릭&f을 이용해 검색 필터를 제거합니다.");
        }
        inv.setslot(16, item.getItem());
        inv.setslot(17, item.getItem());

        item = new ItemClass(new ItemStack(Material.SCUTE), "&6레벨 범위 선택");
        item.setCustomModelData(7);
        item.addLore("&f검색할 아이템의 레벨 범위를 선택합니다.");
        if (levelFilter != null) {
            item.setName(String.format("&6레벨 범위 선택: %.0f ~ %.0f", levelFilter.getFirst(), levelFilter.getSecond()));
            item.addLore("&f ");
            item.addLore("&c쉬프트 우클릭&f을 이용해 검색 필터를 제거합니다.");
        }
        inv.setslot(25, item.getItem());
        inv.setslot(26, item.getItem());

        item = new ItemClass(new ItemStack(Material.SCUTE), "&6정렬 기준 선택");
        item.setCustomModelData(7);
        if (searchType.equals(SearchType.TIME)) item.addLore("&e등록 시간 순서");
        else item.addLore("&7등록 시간 순서");
        if (searchType.equals(SearchType.COST)) item.addLore("&e가격 순서");
        else item.addLore("&7가격 순서");
        if (searchType.equals(SearchType.LEVEL)) item.addLore("&e레벨 순서");
        else item.addLore("&7레벨 순서");
        item.addLore(" ");
        if (reversed) item.addLore("&a정렬 순서 뒤집기 활성화");
        else item.addLore("&7정렬 순서 뒤집기 비활성화");
        item.addLore(" ");
        item.addLore("&f※ 우클릭하면 순서 뒤집기를 ON/OFF 할 수 있습니다.");
        inv.setslot(34, item.getItem());
        inv.setslot(35, item.getItem());

        item = new ItemClass(new ItemStack(Material.SCUTE), "&6내 아이템 관리");
        item.setCustomModelData(7);
        item.addLore("&f현재 장터에 등록한 아이템을 관리합니다.");
        inv.setslot(43, item.getItem());
        inv.setslot(44, item.getItem());

        item = new ItemClass(new ItemStack(Material.SCUTE), String.format("&9이전 페이지 &7[ %d ]", page));
        item.setCustomModelData(7);
        inv.setslot(52, item.getItem());
        item = new ItemClass(new ItemStack(Material.SCUTE), String.format("&c다음 페이지 &7[ %d ]", page));
        item.setCustomModelData(7);
        inv.setslot(53, item.getItem());
    }

    private void openMyItems() {
        this.inv = new InvClass(3, myItemGUI);
        noClick.add(player);
        trueClose = false;

        new BukkitRunnable() {
            @Override
            public void run() {
                try {
                    manager.getMyItems(player, myItems);
                } catch (SQLException e) {
                    e.printStackTrace();
                }
                ArrayList<String> list = new ArrayList<>();
                OfflineMessageManager.getMessages(player.getUniqueId().toString(), list);
                list.removeIf(s -> s.contains("아이템이 판매"));
                OfflineMessageManager.setMessages(player.getUniqueId().toString(), list);
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        PlayerData pdc = new PlayerData(player);
                        int slot = 0;
                        for (MarketItem item : myItems) {
                            inv.setslot(slot, item.getIconOnMyPage());
                            slot++;
                        }
                        if (slot < 27) {
                            if (slot == basicSlots + pdc.getMarketSlot()) {
                                int price = 180 * (pdc.getMarketSlot() + 2) / 2;
                                ItemClass plusIcon = new ItemClass(new ItemStack(Material.SCUTE), "&a슬롯 해금하기");
                                plusIcon.addLore("&f장터에 더 많은 아이템을 올릴 수 있도록 슬롯을 해금하세요!");
                                plusIcon.addLore("&e쉬프트 우클릭&f으로 구매하실 수 있습니다.");
                                plusIcon.addLore(" ");
                                plusIcon.addLore("&b가격: " + price + "루나르");
                                plusIcon.addLore("&7가진 루나르: " + new PaymentData(player).getRunar());
                                plusIcon.setCustomModelData(105);
                                inv.setslot(slot, plusIcon.getItem());
                            } else if (slot > basicSlots + pdc.getMarketSlot()) {
                                ItemClass plusIcon = new ItemClass(new ItemStack(Material.SCUTE), "&4오버 슬롯");
                                plusIcon.setCustomModelData(92);
                                plusIcon.addLore("&f장터 " + (pdc.getMarketSlot() + basicSlots) + "개의 아이템만 등록할 수 있지만");
                                plusIcon.addLore("&f현재 아이템이 너무 많이 등록되어 있습니다.");
                                plusIcon.addLore(" ");
                                plusIcon.addLore("&f더 많은 아이템을 등록하려면 기존 아이템을 철회/판매하고");
                                plusIcon.addLore("&f슬롯을 추가적으로 해금할 수 있습니다.");
                            } else {
                                ItemClass plusIcon = new ItemClass(new ItemStack(Material.SCUTE), "&a새로운 아이템 등록하기");
                                plusIcon.addLore("&f장터에 새로운 아이템을 등록해보세요!");
                                plusIcon.setCustomModelData(29);
                                inv.setslot(slot, plusIcon.getItem());
                            }
                        }
                        if (!disabled) player.openInventory(inv.getInv());
                        trueClose = true;
                        noClick.remove(player);
                    }
                }.runTask(plugin);
            }
        }.runTaskAsynchronously(plugin);
    }

    private void openRegisterWindow() {
        price = 0;
        selectedItem = null;
        trueClose = false;
        inv = new InvClass(6, registerGUI);
        changeRegisterWindow();
        if (!disabled) player.openInventory(inv.getInv());
        trueClose = true;
    }

    private void changeRegisterWindow() {
        inv.getInv().clear();

        if (selectedItem != null) inv.setslot(28, selectedItem);
        else inv.setslot(28, new ItemStack(Material.AIR));

        ItemClass item = new ItemClass(new ItemStack(Material.SCUTE), "&f" + this.price);
        item.setCustomModelData(30);
        inv.setslot(8, item.getItem());


        long price = this.price;
        int slot = 8;
        while (price > 0) {
            long n = price % 10;
            price /= 10;
            item = new ItemClass(new ItemStack(Material.SCUTE), "&f" + this.price);
            item.setCustomModelData((int) (30 + n));
            inv.setslot(slot, item.getItem());
            slot--;
        }

        item = new ItemClass(new ItemStack(Material.SCUTE), "&e아이템 등록하기");
        item.setCustomModelData(7);
        item.addLore("&f클릭하여 아이템을 등록합니다");
        item.addLore(" ");
        item.addLore(String.format("&e등록 수수료: %d 디나르", getCommission(this.price)));
        item.addLore("&7※ 장터에 아이템을 등록하려면 10%의 수수료를 지불해야 합니다.");
        inv.setslot(34, item.getItem());

        item = new ItemClass(new ItemStack(Material.SCUTE), "&f1");
        item.setCustomModelData(7);
        inv.setslot(21, item.getItem());
        item.setName("&f2");
        inv.setslot(22, item.getItem());
        ;
        item.setName("&f3");
        inv.setslot(23, item.getItem());
        item.setName("&f4");
        inv.setslot(30, item.getItem());
        item.setName("&f5");
        inv.setslot(31, item.getItem());
        item.setName("&f6");
        inv.setslot(32, item.getItem());
        item.setName("&f7");
        inv.setslot(39, item.getItem());
        item.setName("&f8");
        inv.setslot(40, item.getItem());
        item.setName("&f9");
        inv.setslot(41, item.getItem());
        item.setName("&f←");
        inv.setslot(48, item.getItem());
        item.setName("&f0");
        inv.setslot(49, item.getItem());
        item.setName("&f00");
        inv.setslot(50, item.getItem());
    }

    /**
     * @param price price
     * @return 수수료 계산 7%
     */
    public long getCommission(long price) {
        return (long) (price / 10f);
    }

    @EventHandler
    public void onInvClose(InventoryCloseEvent e) {
        if (!(e.getPlayer() instanceof Player player)) return;
        if (!player.equals(this.player)) return;

        boolean opened = false;

        if (Msg.recolor(e.getView().getTitle()).equals(mainGUI)) {
            opened = true;
        } else if (Msg.recolor(e.getView().getTitle()).equals(myItemGUI)) {
            opened = true;
        } else if (Msg.recolor(e.getView().getTitle()).equals(registerGUI)) {
            if (selectedItem != null) {
                if (!InvClass.giveOrDrop(player, selectedItem)) {
                    Msg.warn(player, "인벤토리에 공간이 부족하여 장터 등록에 선택된 아이템이 메일로 전송되었습니다.");
                }
            }
            opened = true;
        }
        if (opened) {
            if (trueClose) {
                playerOnWindow.remove(player);
                disable();
            }
        }
    }

    @EventHandler
    public void onClick(InventoryClickEvent e) {
        if (!(e.getWhoClicked() instanceof Player player)) return;
        if (!player.equals(this.player)) return;

        if (clickCool > System.currentTimeMillis()) {
            e.setCancelled(true);
            return;
        }
        clickCool = System.currentTimeMillis() + 200L;

        if (Msg.recolor(e.getView().getTitle()).equals(mainGUI)) {
            e.setCancelled(true);
            if (e.getRawSlot() > 53) return;
            if (noClick.contains(player)) return;

            if (e.getRawSlot() == -999) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.5f);
                disable();
                new MenuWindow(player);
                return;
            }

            if (e.getClick().equals(ClickType.DOUBLE_CLICK)) return;
            else if (e.getRawSlot() == 7 || e.getRawSlot() == 8) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 0.8f);
                if (e.getClick().equals(ClickType.SHIFT_RIGHT)) {
                    nameFilter = null;
                    reloadMainMenu();
                    return;
                }
                trueClose = false;
                player.closeInventory();
                trueClose = true;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        CountDownLatch latch = new CountDownLatch(1);
                        ChatListener listener = new ChatListener(player, latch, 15);
                        Msg.send(player, "&f아이템 이름 또는 이름에 포함되는 단어를 검색해주세요.", pfix);
                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        String answer = listener.getAnswer();
                        if (answer != null) {
                            nameFilter = answer;
                            Msg.send(player, "이름 검색 필터가 적용되었습니다.", pfix);
                        }
                        reloadMainMenu();
                    }
                }.runTaskAsynchronously(plugin);

            } else if (e.getRawSlot() == 16 || e.getRawSlot() == 17) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 0.8f);
                if (e.getClick().equals(ClickType.SHIFT_RIGHT)) {
                    typeFilter = null;
                    reloadMainMenu();
                    return;
                }
                trueClose = false;
                player.closeInventory();
                trueClose = true;
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        CountDownLatch latch = new CountDownLatch(1);
                        ChatListener listener = new ChatListener(player, latch, 15);
                        Msg.send(player, "&f검색할 아이템의 타입을 입력해주세요. ex) 뼈", pfix);
                        Msg.send(player, "&7※ 검색되는 타입에서 하위 속성을 제외하려면 끝에 -를 붙여주세요. ex) 뼈-");
                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        if (listener.getAnswer() != null) {
                            boolean exceptChildType = false;
                            String answer = listener.getAnswer().replace(" ", "");
                            if (answer.endsWith("-")) {
                                answer = answer.replaceAll("-", "");
                                exceptChildType = true;
                            }
                            for (String type : TypeData.getTypeNames()) {
                                if (type.replaceAll(" ", "").equals(answer)) {
                                    typeFilter = type;
                                    break;
                                }
                            }
                            if (typeFilter == null) {
                                Msg.warn(player, "해당 속성은 존재하지 않는 속성입니다.");
                            } else {
                                if (exceptChildType) {
                                    typeFilter += "-";
                                }
                                Msg.send(player, "타입 검색 필터가 적용되었습니다.", pfix);
                            }
                            reloadMainMenu();
                        }
                    }
                }.runTaskAsynchronously(plugin);
            } else if (e.getRawSlot() == 25 || e.getRawSlot() == 26) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 0.8f);
                if (e.getClick().equals(ClickType.SHIFT_RIGHT)) {
                    levelFilter = null;
                    reloadMainMenu();
                    return;
                }
                trueClose = false;
                player.closeInventory();
                trueClose = true;

                new BukkitRunnable() {
                    @Override
                    public void run() {
                        CountDownLatch latch = new CountDownLatch(1);
                        ChatListener listener = new ChatListener(player, latch, 15);

                        Msg.send(player, "&f검색할 아이템의 레벨 범위를 입력해주세요. 입력 형식은 다음과 같습니다.", pfix);
                        Msg.send(player, "&7    → &e\"10~20\" &8(10레벨 부터 20레벨 사이의 아이템을 검색)");
                        Msg.send(player, "&7    → &e\"10~10\" &8(딱 10레벨만 검색)");

                        try {
                            latch.await();
                        } catch (InterruptedException ex) {
                            ex.printStackTrace();
                        }
                        String answer = listener.getAnswer();
                        if (answer != null) {
                            try {
                                int r1, r2;
                                r1 = Integer.parseInt(answer.split("~")[0]);
                                r2 = Integer.parseInt(answer.split("~")[1]);
                                if (r1 > r2) levelFilter = new Pair(r2, r1);
                                else levelFilter = new Pair(r1, r2);
                                Msg.send(player, "레벨 검색 필터가 적용되었습니다.", pfix);
                            } catch (Exception e) {
                                Msg.send(player, "올바른 형식으로 입력해야합니다. &eEx) 10~20, 10~10 등등");
                            }
                        }
                        reloadMainMenu();
                    }
                }.runTaskAsynchronously(plugin);

            } else if (e.getRawSlot() == 34 || e.getRawSlot() == 35) {
                trueClose = false;
                noClick.add(player);
                if (e.getClick().equals(ClickType.RIGHT)) {
                    reversed = !reversed;
                } else {
                    if (searchType.equals(SearchType.TIME)) searchType = SearchType.COST;
                    else if (searchType.equals(SearchType.COST)) searchType = SearchType.LEVEL;
                    else if (searchType.equals(SearchType.LEVEL)) searchType = SearchType.TIME;
                }
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.5f);
                reloadMainMenu();
            } else if (e.getRawSlot() == 43 || e.getRawSlot() == 44) {
                openMyItems();
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.5f);
            } else if (e.getRawSlot() == 52) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                if (page == 1) return;
                lastPage = page;
                page--;
                reloadMainMenu();
            } else if (e.getRawSlot() == 53) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                lastPage = page;
                page++;
                reloadMainMenu();
            } else {
                if (!e.getClick().equals(ClickType.SHIFT_RIGHT)) {
                    Msg.warn(player, "아이템을 구매하려면 &4쉬프트 우클릭&c을 하십시오.");
                    return;
                }

                if (e.getCurrentItem() == null) return;

                int id = (e.getRawSlot() / 9) * 7 + (e.getRawSlot() % 9);
                MarketItem marketItem = searchedList.get(id);

                PlayerData pdc = new PlayerData(player);
                if (pdc.getMoney() < marketItem.getPrice()) {
                    Msg.warn(player, "이 아이템을 구매할 돈이 부족합니다.");
                    return;
                } else if (marketItem.getUuid().equals(player.getUniqueId().toString())) {
                    Msg.warn(player, "자신이 등록한 아이템을 구매할 수 없습니다.", pfix);
                    return;
                }
                noClick.add(player);
                trueClose = false;
                new ProxyCallBack(player, "marketBuy", String.valueOf(marketItem.getTimeID())) {
                    @Override
                    protected void constructExtraByteData(ByteArrayDataOutput byteArrayDataOutput) {
                    }

                    @Override
                    public void done(ByteArrayDataInput in) {
                        if (in.readUTF().equals("fail")) {
                            Msg.warn(player, "잠시 기다리고 다시 시도해주세요.");
                            reloadMainMenu();
                            return;
                        }
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                if (manager.buyItem(player, marketItem.getTimeID())) {
                                    String logMsg;
                                    try {
                                        logMsg = "구매(" + marketItem.getPrice() + ") - " + new ItemData(marketItem.getItem());
                                    } catch (Exception e) {
                                        logMsg = "구매(" + marketItem.getPrice() + ") - " + marketItem.getName();
                                    }
                                    String finalLogMsg = logMsg;
                                    new BukkitRunnable() {
                                        @Override
                                        public void run() {
                                            new ProxyCallBack(player, "marketBuyEnd", String.valueOf(marketItem.getTimeID())) {
                                                @Override
                                                protected void constructExtraByteData(ByteArrayDataOutput byteArrayDataOutput) {
                                                }

                                                @Override
                                                public void done(ByteArrayDataInput byteArrayDataInput) {
                                                    marketItem.give(player);
                                                    Msg.send(player, "성공적으로 아이템을 구매했습니다!", pfix);
                                                    player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1);
                                                    LogManager.log(player, "장터", finalLogMsg);
                                                }
                                            };
                                        }
                                    }.runTask(plugin);
                                }
                                reloadMainMenu();
                            }
                        }.runTaskAsynchronously(plugin);
                    }
                };
            }
        } else if (Msg.recolor(e.getView().getTitle()).equals(myItemGUI)) {
            e.setCancelled(true);
            if (noClick.contains(player)) return;
            if (e.getRawSlot() == -999) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.3f);
                reloadMainMenu();
                return;
            }

            if (e.getClick().equals(ClickType.DOUBLE_CLICK)) return;
            if (e.getCurrentItem() == null) return;
            if (e.getRawSlot() >= 27) return;

            //신규템 등록
            if (e.getCurrentItem().getType().equals(Material.SCUTE)) {
                int cmd = e.getCurrentItem().getItemMeta().getCustomModelData();
                if (cmd == 29) {
                    openRegisterWindow();
                    player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.3f);
                    return;
                } else if (cmd == 105) {
                    if (!e.getClick().equals(ClickType.SHIFT_RIGHT)) return;
                    int price = Integer.parseInt(e.getCurrentItem().getItemMeta().getLore().get(3).split(" ")[1].replaceAll("루나르", "").trim());
                    PaymentData pyd = new PaymentData(player);
                    if (pyd.getRunar() < price) {
                        Msg.warn(player, "디나르가 부족하여 구매할 수 없습니다.");
                        return;
                    }
                    player.playSound(player, Sound.ENTITY_PLAYER_LEVELUP, 1, 2);
                    pyd.setRunar(pyd.getRunar() - price);
                    PlayerData pdc = new PlayerData(player);
                    pdc.setMarketSlot(pdc.getMarketSlot() + 1);
                    trueClose = false;
                    noClick.add(player);
                    player.closeInventory();
                    openMyItems();
                    return;
                } else if (cmd == 92) {
                    return;
                }
            }

            //기존템 삭제

            new BukkitRunnable() {
                @Override
                public void run() {
                    noClick.add(player);
                    trueClose = false;
                    try {
                        manager.getMyItems(player, myItems);
                    } catch (SQLException ex) {
                        ex.printStackTrace();
                    }
                    MarketItem marketItem = myItems.get(e.getRawSlot());
                    if (!marketItem.isBought() && !e.getClick().equals(ClickType.SHIFT_RIGHT)) {
                        noClick.remove(player);
                        trueClose = true;
                        return;
                    }
                    //중복 방지
                    if (manager.isOnPrecess(marketItem.getTimeID())) {
                        Msg.warn(player, "해당 아이템은 이미 구매 처리된 아이템입니다.");
                        openMyItems();
                        return;
                    }
                    new ProxyCallBack(player, "marketBuy", String.valueOf(marketItem.getTimeID() + 1)) {
                        @Override
                        protected void constructExtraByteData(ByteArrayDataOutput byteArrayDataOutput) {

                        }

                        @Override
                        public void done(ByteArrayDataInput in) {
                            if (in.readUTF().equals("fail")) {
                                Msg.warn(player, "잠시만 기다려주세요.");
                                return;
                            }
                            if (marketItem.isBought()) {
                                manager.setOnProcess(marketItem.getTimeID(), true);
                                noClick.add(player);
                                trueClose = false;
                                Msg.send(player, "금액을 수령했습니다!", pfix);
                                PlayerData pdc = new PlayerData(player);
                                pdc.setMoney(pdc.getMoney() + marketItem.getPrice());
                                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.3f);
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        manager.unregisterItem(marketItem);
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                new ProxyCallBack(player, "marketBuyEnd", String.valueOf(marketItem.getTimeID() + 1)) {
                                                    @Override
                                                    protected void constructExtraByteData(ByteArrayDataOutput byteArrayDataOutput) {

                                                    }

                                                    @Override
                                                    public void done(ByteArrayDataInput in) {
                                                        openMyItems();
                                                    }
                                                };

                                            }
                                        }.runTask(plugin);
                                    }
                                }.runTaskAsynchronously(plugin);
                                return;
                            }

                            if (e.getClick().equals(ClickType.SHIFT_RIGHT) || marketItem.isOver()) {
                                manager.setOnProcess(marketItem.getTimeID(), true);
                                noClick.add(player);
                                trueClose = false;
                                new BukkitRunnable() {
                                    @Override
                                    public void run() {
                                        manager.unregisterItem(marketItem);
                                        new BukkitRunnable() {
                                            @Override
                                            public void run() {
                                                new ProxyCallBack(player, "marketBuyEnd", String.valueOf(marketItem.getTimeID() + 1)) {
                                                    @Override
                                                    protected void constructExtraByteData(ByteArrayDataOutput byteArrayDataOutput) {

                                                    }

                                                    @Override
                                                    public void done(ByteArrayDataInput in) {
                                                        openMyItems();
                                                        marketItem.give(player);
                                                        Msg.send(player, "&c등록한 아이템을 철회했습니다.", pfix);
                                                        player.playSound(player, Sound.ENTITY_GENERIC_EXTINGUISH_FIRE, 1, 1.3f);
                                                    }
                                                };
                                            }
                                        }.runTask(plugin);
                                    }
                                }.runTaskAsynchronously(plugin);
                            } else noClick.remove(player);
                        }
                    };
                }
            }.runTaskAsynchronously(plugin);
        } else if (Msg.recolor(e.getView().getTitle()).equals(registerGUI)) {
            e.setCancelled(true);
            if (noClick.contains(player)) return;
            if (e.getRawSlot() == -999) {
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.3f);
                openMyItems();
                return;
            }
            if (e.getClick().equals(ClickType.DOUBLE_CLICK)) return;
            if (e.getCurrentItem() == null) return;

            //아이템 선택
            if (e.getRawSlot() >= 54) {
                if (main.unusableSlots.contains(e.getRawSlot())) return;

                if (selectedItem != null) {
                    Msg.warn(player, "먼저 선택된 아이템을 제거해주세요.");
                    return;
                }
                if (e.getCurrentItem().getType().equals(Material.CHEST)) {
                    Msg.warn(player, "버그를 통해 얻은 아이템은 업로드할 수 없습니다.");
                    return;
                }

                ItemData itemData = new ItemData(e.getCurrentItem());
                if (itemData.getSeason() < RukonPayment.inst().getPassManager().getSeason()) {
                    Msg.warn(player, "이미 시즌이 지난 아이템은 장터에 올릴 수 없습니다.");
                    return;
                }
                if (itemData.isUntradable() || itemData.isQuestItem() || e.getCurrentItem().getType().equals(Material.PAPER) || e.getCurrentItem().getType().equals(Material.ENCHANTED_BOOK)) {
                    if (!(e.getCurrentItem().getItemMeta().getDisplayName().contains("루나르") && (e.getCurrentItem().getItemMeta().hasCustomModelData() && e.getCurrentItem().getItemMeta().getCustomModelData() == 2))) {
                        Msg.warn(player, "이 아이템은 장터에 등록할 수 없습니다.");
                        return;
                    }
                }

                ItemStack item = new ItemStack(e.getCurrentItem());
                item.setAmount(1);
                selectedItem = item;
                item = new ItemStack(e.getCurrentItem());
                item.setAmount(item.getAmount() - 1);
                e.setCurrentItem(item);
                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1.5f);
                changeRegisterWindow();
            }

            //등록된 아이템 제거
            else if (e.getRawSlot() == 28) {
                if (!InvClass.giveOrDrop(player, selectedItem)) {
                    Msg.warn(player, "인벤토리에 공간이 부족하여 아이템이 메일로 전송되었습니다.");
                }
                selectedItem = null;
                changeRegisterWindow();
            }

            //등록
            else if (e.getRawSlot() == 34) {
                if (selectedItem == null) {
                    Msg.warn(player, "아이템을 선택해주세요.");
                    return;
                }
                PlayerData pdc = new PlayerData(player);
                if (price < 15) {
                    Msg.warn(player, "아이템의 가격이 너무 적습니다. 최소 15 디나르 이상이여야 합니다.");
                    return;
                }
                if (pdc.getMoney() < getCommission(price)) {
                    Msg.warn(player, "수수료로 지불할 돈이 부족합니다.");
                    return;
                }
                long deadLine = System.currentTimeMillis() + (86400 * deadLineDays * 1000L);
                if (manager.isOnPrecess(deadLine)) {
                    Msg.warn(player, "다시 시도해주세요.", pfix);
                    return;
                }
                ItemData selectedData = new ItemData(selectedItem);
                MarketItem marketItem = new MarketItem(deadLine, player.getUniqueId().toString(), selectedItem, (int) price, false, Msg.uncolor(selectedData.getName()), selectedData.getLevel(), selectedData.getType());
                manager.setOnProcess(marketItem.getTimeID(), true);
                noClick.add(player);
                trueClose = false;
                selectedItem = null;
                pdc.setMoney((int) (pdc.getMoney() - getCommission(price)));
                new BukkitRunnable() {
                    @Override
                    public void run() {
                        manager.registerNewItem(player, marketItem);
                        String logMsg;
                        try {
                            logMsg = "등록(" + price + ") - " + new ItemData(marketItem.getItem());
                        } catch (Exception e) {
                            logMsg = "등록(" + price + ") - " + marketItem.getName();
                        }
                        String finalLogMsg = logMsg;
                        new BukkitRunnable() {
                            @Override
                            public void run() {
                                LogManager.log(player, "장터", finalLogMsg);
                                Msg.send(player, "새로운 아이템을 등록했습니다.", pfix);
                                player.playSound(player, Sound.ENTITY_EXPERIENCE_ORB_PICKUP, 1, 1.5f);
                            }
                        }.runTask(plugin);
                        noClick.remove(player);
                        openMyItems();
                    }
                }.runTaskAsynchronously(plugin);
            }
            //숫자 입력
            else {
                int n = 0;
                if (e.getRawSlot() == 21) n = 1;
                else if (e.getRawSlot() == 22) n = 2;
                else if (e.getRawSlot() == 23) n = 3;
                else if (e.getRawSlot() == 30) n = 4;
                else if (e.getRawSlot() == 31) n = 5;
                else if (e.getRawSlot() == 32) n = 6;
                else if (e.getRawSlot() == 39) n = 7;
                else if (e.getRawSlot() == 40) n = 8;
                else if (e.getRawSlot() == 41) n = 9;
                else if (e.getRawSlot() == 48) n = -1;
                else if (e.getRawSlot() == 49) n = 0;
                else if (e.getRawSlot() == 50) n = 100;

                if (n >= 0 && n <= 9) {
                    price *= 10;
                    price += n;
                } else if (n == -1) {
                    price /= 10;
                } else if (n == 100) {
                    price *= 100;
                }
                if (price > 999999999) {
                    price = 999999999;
                } else if (price < 0) {
                    price = 0;
                }

                player.playSound(player, Sound.UI_BUTTON_CLICK, 1, 1);
                changeRegisterWindow();
            }
        }
    }

    @EventHandler
    public void onQuit(PlayerQuitEvent e) {
        if (e.getPlayer().equals(player)) disable();
    }

}
