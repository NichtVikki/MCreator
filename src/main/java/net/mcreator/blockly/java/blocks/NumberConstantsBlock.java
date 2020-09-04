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

package net.mcreator.blockly.java.blocks;

import net.mcreator.blockly.BlocklyCompileNote;
import net.mcreator.blockly.BlocklyToCode;
import net.mcreator.blockly.IBlockGenerator;
import net.mcreator.blockly.java.JavaKeywordsMap;
import net.mcreator.util.XMLUtil;
import org.w3c.dom.Element;

public class NumberConstantsBlock implements IBlockGenerator {

	@Override public void generateBlock(BlocklyToCode master, Element block) {
		Element element = XMLUtil.getFirstChildrenWithName("field", block);
		if (element != null && JavaKeywordsMap.MATH_CONSTANTS.get(element.getTextContent()) != null) {
			master.append(JavaKeywordsMap.MATH_CONSTANTS.get(element.getTextContent()));
		} else {
			master.append("0");
			master.addCompileNote(new BlocklyCompileNote(BlocklyCompileNote.Type.WARNING,
					"Failed to find constant value, using 0 as value."));
		}
	}

	@Override public String[] getSupportedBlocks() {
		return new String[] { "math_java_constants" };
	}

	@Override public BlockType getBlockType() {
		return BlockType.OUTPUT;
	}
}
