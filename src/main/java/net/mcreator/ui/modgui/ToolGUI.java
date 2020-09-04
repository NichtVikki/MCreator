/*
 * MCreator (https://mcreator.net/)
 * Copyright (C) 2020 Pylo and contributors
 *
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <https://www.gnu.org/licenses/>.
 */

package net.mcreator.ui.modgui;

import net.mcreator.blockly.Dependency;
import net.mcreator.element.ModElementType;
import net.mcreator.element.parts.TabEntry;
import net.mcreator.element.types.Tool;
import net.mcreator.minecraft.DataListEntry;
import net.mcreator.minecraft.ElementUtil;
import net.mcreator.ui.MCreator;
import net.mcreator.ui.MCreatorApplication;
import net.mcreator.ui.component.SearchableComboBox;
import net.mcreator.ui.component.util.ComboBoxUtil;
import net.mcreator.ui.component.util.ComponentUtils;
import net.mcreator.ui.component.util.PanelUtils;
import net.mcreator.ui.dialogs.BlockItemTextureSelector;
import net.mcreator.ui.help.HelpUtils;
import net.mcreator.ui.laf.renderer.ItemTexturesComboBoxRenderer;
import net.mcreator.ui.laf.renderer.ModelComboBoxRenderer;
import net.mcreator.ui.minecraft.DataListComboBox;
import net.mcreator.ui.minecraft.MCItemListField;
import net.mcreator.ui.minecraft.ProcedureSelector;
import net.mcreator.ui.minecraft.TextureHolder;
import net.mcreator.ui.validation.AggregatedValidationResult;
import net.mcreator.ui.validation.ValidationGroup;
import net.mcreator.ui.validation.component.VTextField;
import net.mcreator.ui.validation.validators.TextFieldValidator;
import net.mcreator.ui.validation.validators.TileHolderValidator;
import net.mcreator.util.ListUtils;
import net.mcreator.util.StringUtils;
import net.mcreator.workspace.elements.ModElement;
import net.mcreator.workspace.resources.Model;
import org.jetbrains.annotations.Nullable;

import javax.swing.*;
import java.awt.*;
import java.net.URI;
import java.net.URISyntaxException;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;

public class ToolGUI extends ModElementGUI<Tool> {

	private TextureHolder texture;

	private final JSpinner harvestLevel = new JSpinner(new SpinnerNumberModel(1, 0, 128000, 1));
	private final JSpinner efficiency = new JSpinner(new SpinnerNumberModel(4, 0, 128000, 0.5));
	private final JSpinner enchantability = new JSpinner(new SpinnerNumberModel(2, 0, 128000, 1));
	private final JSpinner damageVsEntity = new JSpinner(new SpinnerNumberModel(4, 0, 128000, 0.1));
	private final JSpinner attackSpeed = new JSpinner(new SpinnerNumberModel(1, 0, 100, 0.1));
	private final JSpinner usageCount = new JSpinner(new SpinnerNumberModel(100, 0, 128000, 1));

	private final VTextField name = new VTextField(28);

	private final JComboBox<String> toolType = new JComboBox<>(
			new String[] { "Pickaxe", "Axe", "Sword", "Spade", "Hoe", "Shears", "Special", "MultiTool" });

	private final JCheckBox stayInGridWhenCrafting = new JCheckBox("Check to enable");
	private final JCheckBox damageOnCrafting = new JCheckBox("Check to enable");

	private final Model normal = new Model.BuiltInModel("Normal");
	private final SearchableComboBox<Model> renderType = new SearchableComboBox<>(new Model[] { normal });

	private final JCheckBox hasGlow = new JCheckBox("Check to enable");

	private final JTextField specialInfo = new JTextField(20);

	private ProcedureSelector onRightClickedInAir;
	private ProcedureSelector onCrafted;
	private ProcedureSelector onRightClickedOnBlock;
	private ProcedureSelector onBlockDestroyedWithTool;
	private ProcedureSelector onEntityHitWith;
	private ProcedureSelector onItemInInventoryTick;
	private ProcedureSelector onItemInUseTick;
	private ProcedureSelector onStoppedUsing;
	private ProcedureSelector onEntitySwing;

	private MCItemListField blocksAffected;

	private MCItemListField repairItems;

	private final DataListComboBox creativeTab = new DataListComboBox(mcreator);

	private final ValidationGroup page1group = new ValidationGroup();

	public ToolGUI(MCreator mcreator, ModElement modElement, boolean editingMode) {
		super(mcreator, modElement, editingMode);
		this.initGUI();
		super.finalizeGUI();
	}

	@Override protected void initGUI() {
		onRightClickedInAir = new ProcedureSelector(this.withEntry("item/when_right_clicked"), mcreator,
				"When right clicked in air (player loc.)",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack"));
		onCrafted = new ProcedureSelector(this.withEntry("item/on_crafted"), mcreator, "When item is crafted/smelted",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack"));
		onRightClickedOnBlock = new ProcedureSelector(this.withEntry("item/when_right_clicked_block"), mcreator,
				"When right clicked on block (hand loc.)", Dependency.fromString(
				"x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack/direction:direction"));
		onBlockDestroyedWithTool = new ProcedureSelector(this.withEntry("tool/when_block_destroyed"), mcreator,
				"When block destroyed with tool",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack"));
		onEntityHitWith = new ProcedureSelector(this.withEntry("item/when_entity_hit"), mcreator,
				"When living entity is hit with tool", Dependency.fromString(
				"x:number/y:number/z:number/world:world/entity:entity/sourceentity:entity/itemstack:itemstack"));
		onItemInInventoryTick = new ProcedureSelector(this.withEntry("item/inventory_tick"), mcreator,
				"When tool in inventory tick", Dependency
				.fromString("x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack/slot:number"));
		onItemInUseTick = new ProcedureSelector(this.withEntry("item/hand_tick"), mcreator, "When tool in hand tick",
				Dependency.fromString(
						"x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack/slot:number"));
		onStoppedUsing = new ProcedureSelector(this.withEntry("item/when_stopped_using"), mcreator,
				"On player stopped using", Dependency
				.fromString("x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack/time:number"));
		onEntitySwing = new ProcedureSelector(this.withEntry("item/when_entity_swings"), mcreator,
				"When entity swings item",
				Dependency.fromString("x:number/y:number/z:number/world:world/entity:entity/itemstack:itemstack"));

		blocksAffected = new MCItemListField(mcreator, ElementUtil::loadBlocks);

		repairItems = new MCItemListField(mcreator, ElementUtil::loadBlocksAndItems);

		toolType.setRenderer(new ItemTexturesComboBoxRenderer());

		JPanel pane2 = new JPanel(new BorderLayout(10, 10));
		JPanel pane3 = new JPanel(new BorderLayout(10, 10));
		JPanel pane4 = new JPanel(new BorderLayout(10, 10));

		JPanel destal = new JPanel();
		destal.setOpaque(false);

		texture = new TextureHolder(new BlockItemTextureSelector(mcreator, "Item"));
		texture.setOpaque(false);

		hasGlow.setOpaque(false);

		stayInGridWhenCrafting.setOpaque(false);
		damageOnCrafting.setOpaque(false);

		destal.add(ComponentUtils.squareAndBorder(texture, "Tool texture"));

		JPanel rent = new JPanel();
		rent.setLayout(new BoxLayout(rent, BoxLayout.PAGE_AXIS));

		rent.setOpaque(false);
		rent.add(PanelUtils.join(HelpUtils.wrapWithHelpButton(this.withEntry("item/model"),
				new JLabel("<html>Item model:<br><small>Select the item model to be used. Supported: JSON, OBJ")),
				PanelUtils.join(renderType)));

		ComponentUtils.deriveFont(specialInfo, 16);

		renderType.setFont(renderType.getFont().deriveFont(16.0f));
		renderType.setPreferredSize(new Dimension(350, 42));
		renderType.setRenderer(new ModelComboBoxRenderer());

		rent.setBorder(BorderFactory.createTitledBorder(
				BorderFactory.createLineBorder((Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR"), 2), "Tool 3D model",
				0, 0, getFont().deriveFont(12.0f), (Color) UIManager.get("MCreatorLAF.BRIGHT_COLOR")));

		pane2.setOpaque(false);
		pane2.add("Center", PanelUtils.totalCenterInPanel(PanelUtils
				.northAndCenterElement(PanelUtils.join(destal, rent), PanelUtils.gridElements(1, 2, HelpUtils
								.wrapWithHelpButton(this.withEntry("item/special_information"), new JLabel(
										"<html>Special information about the tool:<br><small>Separate entries with comma, to use comma in description use \\,")),
						specialInfo))));

		JPanel selp = new JPanel(new GridLayout(14, 2, 10, 2));
		selp.setOpaque(false);

		ComponentUtils.deriveFont(name, 16);

		harvestLevel.setOpaque(false);
		efficiency.setOpaque(false);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("common/gui_name"), new JLabel("Name in GUI:")));
		selp.add(name);

		selp.add(HelpUtils
				.wrapWithHelpButton(this.withEntry("common/creative_tab"), new JLabel("Creative inventory tab:")));
		selp.add(creativeTab);

		selp.add(HelpUtils
				.wrapWithHelpButton(this.withEntry("item/glowing_effect"), new JLabel("Enable glowing effect")));
		selp.add(hasGlow);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("tool/type"), new JLabel("Type:")));
		selp.add(toolType);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("tool/harvest_level"), new JLabel("Harvest level:")));
		selp.add(harvestLevel);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("tool/efficiency"), new JLabel("Efficiency:")));
		selp.add(efficiency);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("item/enchantability"), new JLabel("Enchantability:")));
		selp.add(enchantability);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("tool/attack_speed"), new JLabel("Attack speed:")));
		selp.add(attackSpeed);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("item/damage_vs_entity"),
				new JLabel("Damage vs mob/animal (melee damage):")));
		selp.add(damageVsEntity);

		selp.add(HelpUtils
				.wrapWithHelpButton(this.withEntry("item/number_of_uses"), new JLabel("Number of uses / durability:")));
		selp.add(usageCount);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("tool/repair_items"), new JLabel("Repair items: ")));
		selp.add(repairItems);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("tool/blocks_affected"), new JLabel("Blocks affected: ")));
		selp.add(blocksAffected);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("item/container_item"),
				new JLabel("Does item stay in crafting grid when crafted?")));
		selp.add(stayInGridWhenCrafting);

		selp.add(HelpUtils.wrapWithHelpButton(this.withEntry("item/container_item_damage"), new JLabel(
				"<html>Damage item instead on crafting<br><small>Make sure to enable \"stay in crafting grid\" and that item is damageable")));
		selp.add(damageOnCrafting);

		blocksAffected.setEnabled(false);

		toolType.addActionListener(event -> {
			if (toolType.getSelectedItem() != null)
				blocksAffected.setEnabled(toolType.getSelectedItem().equals("Special"));
		});

		pane4.setOpaque(false);

		pane4.add("Center", PanelUtils.totalCenterInPanel(selp));

		pane3.setOpaque(false);

		JPanel events = new JPanel(new GridLayout(3, 3, 10, 10));
		events.add(onRightClickedInAir);
		events.add(onRightClickedOnBlock);
		events.add(onCrafted);
		events.add(onBlockDestroyedWithTool);
		events.add(onEntityHitWith);
		events.add(onItemInInventoryTick);
		events.add(onItemInUseTick);
		events.add(onStoppedUsing);
		events.add(onEntitySwing);
		events.setOpaque(false);
		pane3.add(PanelUtils.totalCenterInPanel(events));

		texture.setValidator(new TileHolderValidator(texture));

		page1group.addValidationElement(texture);

		name.setValidator(new TextFieldValidator(name, "Tool needs a name"));
		name.enableRealtimeValidation();

		addPage("Visual", pane2);
		addPage("Properties", pane4);
		addPage("Triggers", pane3);

		if (!isEditingMode()) {
			String readableNameFromModElement = StringUtils.machineToReadableName(modElement.getName());
			name.setText(readableNameFromModElement);
		}
	}

	@Override public void reloadDataLists() {
		super.reloadDataLists();
		onRightClickedInAir.refreshListKeepSelected();
		onCrafted.refreshListKeepSelected();
		onRightClickedOnBlock.refreshListKeepSelected();
		onBlockDestroyedWithTool.refreshListKeepSelected();
		onEntityHitWith.refreshListKeepSelected();
		onItemInInventoryTick.refreshListKeepSelected();
		onItemInUseTick.refreshListKeepSelected();
		onStoppedUsing.refreshListKeepSelected();
		onEntitySwing.refreshListKeepSelected();

		ComboBoxUtil.updateComboBoxContents(creativeTab, ElementUtil.loadAllTabs(mcreator.getWorkspace()),
				new DataListEntry.Dummy("TOOLS"));

		ComboBoxUtil.updateComboBoxContents(renderType, ListUtils.merge(Collections.singletonList(normal),
				Model.getModelsWithTextureMaps(mcreator.getWorkspace()).stream()
						.filter(el -> el.getType() == Model.Type.JSON || el.getType() == Model.Type.OBJ)
						.collect(Collectors.toList())));
	}

	@Override protected AggregatedValidationResult validatePage(int page) {
		if (page == 1)
			return new AggregatedValidationResult(name);
		else if (page == 0)
			return new AggregatedValidationResult(page1group);
		return new AggregatedValidationResult.PASS();
	}

	@Override public void openInEditingMode(Tool tool) {
		creativeTab.setSelectedItem(tool.creativeTab);
		name.setText(tool.name);
		texture.setTextureFromTextureName(tool.texture);
		toolType.setSelectedItem(tool.toolType);
		harvestLevel.setValue(tool.harvestLevel);
		efficiency.setValue(tool.efficiency);
		enchantability.setValue(tool.enchantability);
		attackSpeed.setValue(tool.attackSpeed);
		damageVsEntity.setValue(tool.damageVsEntity);
		usageCount.setValue(tool.usageCount);
		onRightClickedInAir.setSelectedProcedure(tool.onRightClickedInAir);
		onRightClickedOnBlock.setSelectedProcedure(tool.onRightClickedOnBlock);
		onCrafted.setSelectedProcedure(tool.onCrafted);
		onBlockDestroyedWithTool.setSelectedProcedure(tool.onBlockDestroyedWithTool);
		onEntityHitWith.setSelectedProcedure(tool.onEntityHitWith);
		onItemInInventoryTick.setSelectedProcedure(tool.onItemInInventoryTick);
		onItemInUseTick.setSelectedProcedure(tool.onItemInUseTick);
		onStoppedUsing.setSelectedProcedure(tool.onStoppedUsing);
		onEntitySwing.setSelectedProcedure(tool.onEntitySwing);
		hasGlow.setSelected(tool.hasGlow);
		repairItems.setListElements(tool.repairItems);
		specialInfo.setText(
				tool.specialInfo.stream().map(info -> info.replace(",", "\\,")).collect(Collectors.joining(",")));
		stayInGridWhenCrafting.setSelected(tool.stayInGridWhenCrafting);
		damageOnCrafting.setSelected(tool.damageOnCrafting);

		blocksAffected.setListElements(tool.blocksAffected);

		if (toolType.getSelectedItem() != null)
			blocksAffected.setEnabled(toolType.getSelectedItem().equals("Special"));

		Model model = tool.getItemModel();
		if (model != null)
			renderType.setSelectedItem(model);
	}

	@Override public Tool getElementFromGUI() {
		Tool tool = new Tool(modElement);
		tool.name = name.getText();
		tool.creativeTab = new TabEntry(mcreator.getWorkspace(), creativeTab.getSelectedItem());
		tool.toolType = (String) toolType.getSelectedItem();
		tool.harvestLevel = (int) harvestLevel.getValue();
		tool.efficiency = (double) efficiency.getValue();
		tool.enchantability = (int) enchantability.getValue();
		tool.attackSpeed = (double) attackSpeed.getValue();
		tool.damageVsEntity = (double) damageVsEntity.getValue();
		tool.usageCount = (int) usageCount.getValue();
		tool.blocksAffected = blocksAffected.getListElements();
		tool.onRightClickedInAir = onRightClickedInAir.getSelectedProcedure();
		tool.onRightClickedOnBlock = onRightClickedOnBlock.getSelectedProcedure();
		tool.onCrafted = onCrafted.getSelectedProcedure();
		tool.onBlockDestroyedWithTool = onBlockDestroyedWithTool.getSelectedProcedure();
		tool.onEntityHitWith = onEntityHitWith.getSelectedProcedure();
		tool.onItemInInventoryTick = onItemInInventoryTick.getSelectedProcedure();
		tool.onItemInUseTick = onItemInUseTick.getSelectedProcedure();
		tool.onStoppedUsing = onStoppedUsing.getSelectedProcedure();
		tool.onEntitySwing = onEntitySwing.getSelectedProcedure();
		tool.hasGlow = hasGlow.isSelected();
		tool.repairItems = repairItems.getListElements();
		tool.specialInfo = StringUtils.splitCommaSeparatedStringListWithEscapes(specialInfo.getText());

		tool.stayInGridWhenCrafting = stayInGridWhenCrafting.isSelected();
		tool.damageOnCrafting = damageOnCrafting.isSelected();

		tool.texture = texture.getID();

		Model.Type modelType = (Objects.requireNonNull(renderType.getSelectedItem())).getType();
		tool.renderType = 0;
		if (modelType == Model.Type.JSON)
			tool.renderType = 1;
		else if (modelType == Model.Type.OBJ)
			tool.renderType = 2;
		tool.customModelName = (Objects.requireNonNull(renderType.getSelectedItem())).getReadableName();

		return tool;
	}

	@Override public @Nullable URI getContextURL() throws URISyntaxException {
		return new URI(MCreatorApplication.SERVER_DOMAIN + "/wiki/how-make-tool");
	}
}
