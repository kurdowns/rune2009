package plugin.ai.general.scriptrepository

import core.game.node.item.Item
import core.tools.ItemNames
import plugin.ai.skillingbot.SkillingBotAssembler
import plugin.skill.Skills

class NonBankingMiner : Script() {
    override fun tick() {
        val rock = scriptAPI.getNearestNode(11957,true)
        if(rock != null){
            rock.interaction.handle(bot,rock.interaction[0])
            if(bot.inventory.isFull)
                bot.inventory.clear()
        }
    }

    override fun newInstance(): Script {
        val script = NonBankingMiner()
        script.bot = SkillingBotAssembler().produce(SkillingBotAssembler.Wealth.POOR,bot.startLocation)
        return script
    }

    init {
        skills[Skills.ATTACK] = 99
        equipment.add(Item(ItemNames.MITHRIL_PICKAXE))
        inventory.add(Item(ItemNames.MITHRIL_PICKAXE))
        skills[Skills.MINING] = 50
    }
}