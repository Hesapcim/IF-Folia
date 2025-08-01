package com.github.stefvanschie.inventoryframework.gui.type;

import com.github.stefvanschie.inventoryframework.HumanEntityCache;
import com.github.stefvanschie.inventoryframework.adventuresupport.TextHolder;
import com.github.stefvanschie.inventoryframework.exception.XMLLoadException;
import com.github.stefvanschie.inventoryframework.gui.GuiItem;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.InventoryBased;
import com.github.stefvanschie.inventoryframework.gui.type.util.MergedGui;
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui;
import com.github.stefvanschie.inventoryframework.pane.Pane;
import org.bukkit.entity.HumanEntity;
import org.bukkit.event.inventory.InventoryClickEvent;
import org.bukkit.event.inventory.InventoryType;
import org.bukkit.inventory.Inventory;
import org.bukkit.plugin.Plugin;
import org.bukkit.plugin.java.JavaPlugin;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;
import org.w3c.dom.Document;
import org.w3c.dom.Element;
import org.w3c.dom.Node;
import org.w3c.dom.NodeList;
import org.xml.sax.SAXException;

import javax.xml.parsers.DocumentBuilderFactory;
import javax.xml.parsers.ParserConfigurationException;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Represents a gui in the form of a barrel.
 *
 * @since 0.8.0
 */
public class BarrelGui extends NamedGui implements MergedGui, InventoryBased {

    /**
     * Represents the inventory component for the entire gui
     */
    @NotNull
    private InventoryComponent inventoryComponent = new InventoryComponent(9, 7);

    /**
     * Constructs a new GUI
     *
     * @param title the title/name of this gui.
     * @since 0.8.0
     */
    public BarrelGui(@NotNull String title) {
        super(title);
    }

    /**
     * Constructs a new GUI
     *
     * @param title the title/name of this gui.
     * @since 0.10.0
     */
    public BarrelGui(@NotNull TextHolder title) {
        super(title);
    }

    /**
     * Constructs a new barrel gui for the given {@code plugin}.
     *
     * @param title the title/name of this gui.
     * @param plugin the owning plugin of this gui
     * @see #BarrelGui(String)
     * @since 0.10.8
     */
    public BarrelGui(@NotNull String title, @NotNull Plugin plugin) {
        super(title, plugin);
    }

    /**
     * Constructs a new barrel gui for the given {@code plugin}.
     *
     * @param title the title/name of this gui.
     * @param plugin the owning plugin of this gui
     * @see #BarrelGui(TextHolder)
     * @since 0.10.8
     */
    public BarrelGui(@NotNull TextHolder title, @NotNull Plugin plugin) {
        super(title, plugin);
    }

    @Override
    public void show(@NotNull HumanEntity humanEntity) {
        if (isDirty()) {
            this.inventory = createInventory();
            markChanges();
        }

        getInventory().clear();

        int height = getInventoryComponent().getHeight();

        getInventoryComponent().display();

        InventoryComponent topComponent = getInventoryComponent().excludeRows(height - 4, height - 1);
        InventoryComponent bottomComponent = getInventoryComponent().excludeRows(0, height - 5);

        topComponent.placeItems(getInventory(), 0);

        if (bottomComponent.hasItem()) {
            HumanEntityCache humanEntityCache = getHumanEntityCache();

            if (!humanEntityCache.contains(humanEntity)) {
                humanEntityCache.storeAndClear(humanEntity);
            }

            bottomComponent.placeItems(humanEntity.getInventory(), 0);
        }

        // Use Folia-compatible scheduling for inventory opening
        getFoliaScheduler().runAtEntity(humanEntity, () -> {
            humanEntity.openInventory(getInventory());
        });
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public BarrelGui copy() {
        BarrelGui gui = new BarrelGui(getTitleHolder(), super.plugin);

        gui.inventoryComponent = inventoryComponent.copy();

        gui.setOnTopClick(this.onTopClick);
        gui.setOnBottomClick(this.onBottomClick);
        gui.setOnGlobalClick(this.onGlobalClick);
        gui.setOnOutsideClick(this.onOutsideClick);
        gui.setOnClose(this.onClose);

        return gui;
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        if (this.inventory == null) {
            this.inventory = createInventory();
        }

        return inventory;
    }

    @Override
    public void click(@NotNull InventoryClickEvent event) {
        getInventoryComponent().click(this, event, event.getRawSlot());
    }

    @Contract(pure = true)
    @Override
    public boolean isPlayerInventoryUsed() {
        return getInventoryComponent().excludeRows(0, getInventoryComponent().getHeight() - 5).hasItem();
    }

    @Override
    public void addPane(@NotNull Pane pane) {
        this.inventoryComponent.addPane(pane);
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public List<Pane> getPanes() {
        return this.inventoryComponent.getPanes();
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public Collection<GuiItem> getItems() {
        return getPanes().stream().flatMap(pane -> pane.getItems().stream()).collect(Collectors.toSet());
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public Inventory createInventory() {
        return getTitleHolder().asInventoryTitle(this, InventoryType.BARREL);
    }

    @Contract(pure = true)
    @Override
    public int getViewerCount() {
        return getInventory().getViewers().size();
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public List<HumanEntity> getViewers() {
        return new ArrayList<>(getInventory().getViewers());
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public InventoryComponent getInventoryComponent() {
        return inventoryComponent;
    }

    /**
     * Loads a barrel gui from an XML file.
     *
     * @param instance the instance on which to reference fields and methods
     * @param inputStream the input stream containing the XML data
     * @param plugin the plugin that will be the owner of the created gui
     * @return the loaded barrel gui
     * @see #load(Object, InputStream)
     * @since 0.10.8
     */
    @Nullable
    @Contract(pure = true)
    public static BarrelGui load(@NotNull Object instance, @NotNull InputStream inputStream, @NotNull Plugin plugin) {
        try {
            Document document = DocumentBuilderFactory.newInstance().newDocumentBuilder().parse(inputStream);
            Element documentElement = document.getDocumentElement();

            documentElement.normalize();

            return load(instance, documentElement, plugin);
        } catch (SAXException | ParserConfigurationException | IOException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Loads a barrel gui from the specified element, applying code references to the provided instance.
     *
     * @param instance the instance on which to reference fields and methods
     * @param element the element to load the gui from
     * @param plugin the plugin that will be the owner of the created gui
     * @return the loaded barrel gui
     * @see #load(Object, Element)
     * @since 0.10.8
     */
    @NotNull
    public static BarrelGui load(@NotNull Object instance, @NotNull Element element, @NotNull Plugin plugin) {
        if (!element.hasAttribute("title")) {
            throw new XMLLoadException("Provided XML element's gui tag doesn't have the mandatory title attribute set");
        }

        BarrelGui barrelGui = new BarrelGui(element.getAttribute("title"), plugin);
        barrelGui.initializeOrThrow(instance, element);

        if (element.hasAttribute("populate")) {
            return barrelGui;
        }

        NodeList childNodes = element.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node item = childNodes.item(index);

            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element componentElement = (Element) item;
            InventoryComponent inventoryComponent = barrelGui.getInventoryComponent();

            if (componentElement.getTagName().equalsIgnoreCase("component")) {
                inventoryComponent.load(instance, componentElement, plugin);
            } else {
                inventoryComponent.load(instance, element, plugin);
            }

            break;
        }

        return barrelGui;
    }

    /**
     * Loads a barrel gui from an XML file.
     *
     * @param instance the instance on which to reference fields and methods
     * @param inputStream the input stream containing the XML data
     * @return the loaded barrel gui
     * @since 0.8.0
     */
    @Nullable
    @Contract(pure = true)
    public static BarrelGui load(@NotNull Object instance, @NotNull InputStream inputStream) {
        return load(instance, inputStream, JavaPlugin.getProvidingPlugin(BarrelGui.class));
    }

    /**
     * Loads a barrel gui from the specified element, applying code references to the provided instance.
     *
     * @param instance the instance on which to reference fields and methods
     * @param element the element to load the gui from
     * @return the loaded barrel gui
     * @since 0.8.0
     */
    @NotNull
    public static BarrelGui load(@NotNull Object instance, @NotNull Element element) {
        return load(instance, element, JavaPlugin.getProvidingPlugin(BarrelGui.class));
    }
}
