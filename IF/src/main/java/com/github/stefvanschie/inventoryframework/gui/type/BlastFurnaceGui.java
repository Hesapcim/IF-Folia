package com.github.stefvanschie.inventoryframework.gui.type;

import com.github.stefvanschie.inventoryframework.HumanEntityCache;
import com.github.stefvanschie.inventoryframework.adventuresupport.TextHolder;
import com.github.stefvanschie.inventoryframework.exception.XMLLoadException;
import com.github.stefvanschie.inventoryframework.gui.InventoryComponent;
import com.github.stefvanschie.inventoryframework.gui.type.util.InventoryBased;
import com.github.stefvanschie.inventoryframework.gui.type.util.NamedGui;
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
import java.util.List;

/**
 * Represents a gui in the form of a blast furnace
 *
 * @since 0.8.0
 */
public class BlastFurnaceGui extends NamedGui implements InventoryBased {

    /**
     * Represents the inventory component for the ingredient
     */
    @NotNull
    private InventoryComponent ingredientComponent = new InventoryComponent(1, 1);

    /**
     * Represents the inventory component for the fuel
     */
    @NotNull
    private InventoryComponent fuelComponent = new InventoryComponent(1, 1);

    /**
     * Represents the inventory component for the output
     */
    @NotNull
    private InventoryComponent outputComponent = new InventoryComponent(1, 1);

    /**
     * Represents the inventory component for the player inventory
     */
    @NotNull
    private InventoryComponent playerInventoryComponent = new InventoryComponent(9, 4);

    /**
     * Constructs a new GUI
     *
     * @param title the title/name of this gui.
     * @since 0.8.0
     */
    public BlastFurnaceGui(@NotNull String title) {
        super(title);
    }

    /**
     * Constructs a new GUI
     *
     * @param title the title/name of this gui.
     * @since 0.10.0
     */
    public BlastFurnaceGui(@NotNull TextHolder title) {
        super(title);
    }

    /**
     * Constructs a new blast furnace gui for the given {@code plugin}.
     *
     * @param title the title/name of this gui.
     * @param plugin the owning plugin of this gui
     * @see #BlastFurnaceGui(String)
     * @since 0.10.8
     */
    public BlastFurnaceGui(@NotNull String title, @NotNull Plugin plugin) {
        super(title, plugin);
    }

    /**
     * Constructs a new blast furnace gui for the given {@code plugin}.
     *
     * @param title the title/name of this gui.
     * @param plugin the owning plugin of this gui
     * @see #BlastFurnaceGui(TextHolder)
     * @since 0.10.8
     */
    public BlastFurnaceGui(@NotNull TextHolder title, @NotNull Plugin plugin) {
        super(title, plugin);
    }

    @Override
    public void show(@NotNull HumanEntity humanEntity) {
        if (isDirty()) {
            this.inventory = createInventory();
            markChanges();
        }

        getInventory().clear();

        getIngredientComponent().display(getInventory(), 0);
        getFuelComponent().display(getInventory(), 1);
        getOutputComponent().display(getInventory(), 2);
        getPlayerInventoryComponent().display();

        if (getPlayerInventoryComponent().hasItem()) {
            HumanEntityCache humanEntityCache = getHumanEntityCache();

            if (!humanEntityCache.contains(humanEntity)) {
                humanEntityCache.storeAndClear(humanEntity);
            }

            getPlayerInventoryComponent().placeItems(humanEntity.getInventory(), 0);
        }

        // Use Folia-compatible scheduling for inventory opening
        getFoliaScheduler().runAtEntity(humanEntity, () -> {
            humanEntity.openInventory(getInventory());
        });
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public BlastFurnaceGui copy() {
        BlastFurnaceGui gui = new BlastFurnaceGui(getTitleHolder(), super.plugin);

        gui.ingredientComponent = ingredientComponent.copy();
        gui.fuelComponent = fuelComponent.copy();
        gui.outputComponent = outputComponent.copy();
        gui.playerInventoryComponent = playerInventoryComponent.copy();

        gui.setOnTopClick(this.onTopClick);
        gui.setOnBottomClick(this.onBottomClick);
        gui.setOnGlobalClick(this.onGlobalClick);
        gui.setOnOutsideClick(this.onOutsideClick);
        gui.setOnClose(this.onClose);

        return gui;
    }

    @Override
    public void click(@NotNull InventoryClickEvent event) {
        int rawSlot = event.getRawSlot();

        if (rawSlot == 0) {
            getIngredientComponent().click(this, event, 0);
        } else if (rawSlot == 1) {
            getFuelComponent().click(this, event, 0);
        } else if (rawSlot == 2) {
            getOutputComponent().click(this, event, 0);
        } else {
            getPlayerInventoryComponent().click(this, event, rawSlot - 3);
        }
    }

    @NotNull
    @Override
    public Inventory getInventory() {
        if (this.inventory == null) {
            this.inventory = createInventory();
        }

        return inventory;
    }

    @Contract(pure = true)
    @Override
    public boolean isPlayerInventoryUsed() {
        return getPlayerInventoryComponent().hasItem();
    }

    @NotNull
    @Contract(pure = true)
    @Override
    public Inventory createInventory() {
        return getTitleHolder().asInventoryTitle(this, InventoryType.BLAST_FURNACE);
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

    /**
     * Gets the inventory component representing the ingredient
     *
     * @return the ingredient component
     * @since 0.8.0
     */
    @NotNull
    @Contract(pure = true)
    public InventoryComponent getIngredientComponent() {
        return ingredientComponent;
    }

    /**
     * Gets the inventory component representing the fuel
     *
     * @return the fuel component
     * @since 0.8.0
     */
    @NotNull
    @Contract(pure = true)
    public InventoryComponent getFuelComponent() {
        return fuelComponent;
    }

    /**
     * Gets the inventory component representing the output
     *
     * @return the output component
     * @since 0.8.0
     */
    @NotNull
    @Contract(pure = true)
    public InventoryComponent getOutputComponent() {
        return outputComponent;
    }

    /**
     * Gets the inventory component representing the player inventory
     *
     * @return the player inventory component
     * @since 0.8.0
     */
    @NotNull
    @Contract(pure = true)
    public InventoryComponent getPlayerInventoryComponent() {
        return playerInventoryComponent;
    }

    /**
     * Loads a blast furnace gui from an XML file.
     *
     * @param instance the instance on which to reference fields and methods
     * @param inputStream the input stream containing the XML data
     * @param plugin the plugin that will be the owner of the created gui
     * @return the loaded blast furnace gui
     * @see #load(Object, InputStream)
     * @since 0.10.8
     */
    @Nullable
    @Contract(pure = true)
    public static BlastFurnaceGui load(@NotNull Object instance, @NotNull InputStream inputStream,
                                       @NotNull Plugin plugin) {
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
     * Loads a blast furnace gui from the specified element, applying code references to the provided instance.
     *
     * @param instance the instance on which to reference fields and methods
     * @param element the element to load the gui from
     * @param plugin the plugin that will be the owner of the created gui
     * @return the loaded blast furnace gui
     * @see #load(Object, Element)
     * @since 0.10.8
     */
    @NotNull
    public static BlastFurnaceGui load(@NotNull Object instance, @NotNull Element element, @NotNull Plugin plugin) {
        if (!element.hasAttribute("title")) {
            throw new XMLLoadException("Provided XML element's gui tag doesn't have the mandatory title attribute set");
        }

        BlastFurnaceGui blastFurnaceGui = new BlastFurnaceGui(element.getAttribute("title"), plugin);
        blastFurnaceGui.initializeOrThrow(instance, element);

        if (element.hasAttribute("populate")) {
            return blastFurnaceGui;
        }

        NodeList childNodes = element.getChildNodes();

        for (int index = 0; index < childNodes.getLength(); index++) {
            Node item = childNodes.item(index);

            if (item.getNodeType() != Node.ELEMENT_NODE) {
                continue;
            }

            Element componentElement = (Element) item;

            if (!componentElement.getTagName().equalsIgnoreCase("component")) {
                throw new XMLLoadException("Gui element contains non-component tags");
            }

            if (!componentElement.hasAttribute("name")) {
                throw new XMLLoadException("Component tag does not have a name specified");
            }

            InventoryComponent component;

            switch (componentElement.getAttribute("name")) {
                case "ingredient":
                    component = blastFurnaceGui.getIngredientComponent();
                    break;
                case "fuel":
                    component = blastFurnaceGui.getFuelComponent();
                    break;
                case "output":
                    component = blastFurnaceGui.getOutputComponent();
                    break;
                case "player-inventory":
                    component = blastFurnaceGui.getPlayerInventoryComponent();
                    break;
                default:
                    throw new XMLLoadException("Unknown component name");
            }

            component.load(instance, componentElement, plugin);
        }

        return blastFurnaceGui;
    }

    /**
     * Loads a blast furnace gui from an XML file.
     *
     * @param instance the instance on which to reference fields and methods
     * @param inputStream the input stream containing the XML data
     * @return the loaded blast furnace gui
     * @since 0.8.0
     */
    @Nullable
    @Contract(pure = true)
    public static BlastFurnaceGui load(@NotNull Object instance, @NotNull InputStream inputStream) {
        return load(instance, inputStream, JavaPlugin.getProvidingPlugin(BlastFurnaceGui.class));
    }

    /**
     * Loads a blast furnace gui from the specified element, applying code references to the provided instance.
     *
     * @param instance the instance on which to reference fields and methods
     * @param element the element to load the gui from
     * @return the loaded blast furnace gui
     * @since 0.8.0
     */
    @NotNull
    public static BlastFurnaceGui load(@NotNull Object instance, @NotNull Element element) {
        return load(instance, element, JavaPlugin.getProvidingPlugin(BlastFurnaceGui.class));
    }
}
