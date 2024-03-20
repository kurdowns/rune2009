package plugin.ai.general

import core.cache.def.impl.ItemDefinition
import core.game.interaction.MovementPulse
import core.game.node.Node
import core.game.node.`object`.GameObject
import core.game.node.entity.Entity
import core.game.node.entity.npc.NPC
import core.game.node.entity.player.Player
import core.game.node.entity.player.info.PlayerDetails
import core.game.node.item.GroundItem
import core.game.node.item.GroundItemManager
import core.game.node.item.Item
import core.game.system.SystemLogger
import core.game.system.task.Pulse
import core.game.world.GameWorld
import core.game.world.map.Location
import core.game.world.map.RegionManager
import core.game.world.map.path.Path
import core.game.world.map.path.Pathfinder
import core.game.world.update.flag.context.Animation
import core.game.world.update.flag.context.Graphics
import core.tools.ItemNames
import core.tools.RandomFunction
import plugin.ai.AIPlayer
import plugin.ai.AIRepository
import plugin.ai.general.scriptrepository.LobsterCatcher
import plugin.ai.general.scriptrepository.SeersMagicTrees
import plugin.consumable.Consumable
import plugin.consumable.Consumables
import plugin.consumable.Food
import plugin.consumable.effects.HealingEffect
import plugin.ge.BotGrandExchange
import plugin.ge.GEOfferDispatch
import plugin.ge.GrandExchangeOffer
import plugin.ge.OfferState
import plugin.skill.Skills
import java.util.ArrayList
import java.util.concurrent.CountDownLatch
import java.util.concurrent.Executors
import kotlin.math.abs
import kotlin.math.pow
import kotlin.math.sqrt

class ScriptAPI(private val bot: Player) {
    val GRAPHICSUP = Graphics(1576)
    val ANIMATIONUP = Animation(8939)
    val GRAPHICSDOWN = Graphics(1577)
    val ANIMATIONDOWN = Animation(8941)

    /**
     * Gets the distance between two nodes
     * @param n1 the first node
     * @param n2 the second node
     * @author Ceikry
     */
    fun distance(n1: Node, n2: Node): Double {
        return sqrt((n1.location.x - n2.location.x.toDouble()).pow(2.0) + (n2.location.y - n1.location.y.toDouble()).pow(2.0))
    }

    /**
     * Gets the nearest node with name entityName
     * @param entityName the name of the node to look for
     * @return the nearest node with a matching name or null
     * @author Ceikry
     */
    fun getNearestNode(entityName: String): Node? {
        var entity: Node? = null
        var minDistance = Double.MAX_VALUE
        for (node in RegionManager.forId(bot.location.regionId).planes[bot.location.z].entities) {
            if (node != null && node.name == entityName && distance(bot, node) < minDistance && !Pathfinder.find(bot, node).isMoveNear) {
                entity = node
                minDistance = distance(bot, node)
            }
        }
        return entity
    }

    /**
     * Gets the nearest node with matching id.
     * @param id the id to look for
     * @param object whether or not the node we are looking for is an object.
     * @return the closest node with matching id or null.
     * @author Ceikry
     */
    fun getNearestNode(id: Int, `object`: Boolean): Node? {
        if (`object`) {
            var entity: Node? = null
            var minDistance = Double.MAX_VALUE
            for (objects in RegionManager.forId(bot.location.regionId).planes[bot.location.z].objects) {
                for(e in objects) {
                    if (e != null && e.id == id && distance(bot, e) < minDistance && !Pathfinder.find(bot, e).isMoveNear && e.isActive) {
                        entity = e
                        minDistance = distance(bot, e)
                    }
                }
            }
            return if(entity == null) null else entity as GameObject
        } else {
            var entity: Node? = null
            var minDistance = Double.MAX_VALUE
            for (e in RegionManager.forId(bot.location.regionId).planes[bot.location.z].entities) {
                if (e != null && e.id == id && distance(bot, e) < minDistance && !Pathfinder.find(bot, e).isMoveNear) {
                    entity = e
                    minDistance = distance(bot, e)
                }
            }
            return entity
        }
    }

    /**
     * Gets the nearest node with a matching name.
     * @param name the name to look for.
     * @param object whether or not the node we are looking for is an object.
     * @return the nearest matching node or null.
     * @author Ceikry
     */
    fun getNearestNode(name: String, `object`: Boolean): Node? {
        if (`object`) {
            var entity: Node? = null
            var minDistance = Double.MAX_VALUE
            for (objects in RegionManager.forId(bot.location.regionId).planes[bot.location.z].objects) {
                for(e in objects) {
                    if (e != null && e.name.toLowerCase() == name.toLowerCase() && distance(bot, e) < minDistance && !Pathfinder.find(bot, e).isMoveNear && e.isActive) {
                        entity = e
                        minDistance = distance(bot, e)
                    }
                }
            }
            return if(entity == null) null else entity as GameObject
        } else {
            var entity: Node? = null
            var minDistance = Double.MAX_VALUE
            for (e in RegionManager.forId(bot.location.regionId).planes[bot.location.z].entities) {
                if (e != null && e.name.toLowerCase() == name.toLowerCase() && distance(bot, e) < minDistance && !Pathfinder.find(bot, e).isMoveNear) {
                    entity = e
                    minDistance = distance(bot, e)
                }
            }
            return entity
        }
    }

    /**
     * Gets the nearest ground item with matching ID from the list in AIRepository.
     * @param id the id of the ground item we are looking for.
     * @return the nearest GroundItem with a matching id or null
     * @author Ceikry
     */
    private fun getNearestGroundItem(id: Int): GroundItem? {
        var distance = 11.0
        var closest: GroundItem? = null
        if(AIRepository.getItems(bot) == null) return null
        for(item in AIRepository.getItems(bot)!!.filter { it.distance(bot.location) < 10 }){
            if(item.id == id){
                //distance = item.distance(bot.location)
                closest = item
            }
        }
        if(!GroundItemManager.getItems().contains(closest)) AIRepository.getItems(bot)?.remove(closest).also {return null}
        return closest
    }

    /**
     * Takes the nearest ground item with a matching id if it exists.
     * @param id the id to look for
     * @author Ceikry
     */
    fun takeNearestGroundItem(id: Int){
        val item = getNearestGroundItem(id)
        if(item != null)
            item.interaction?.handle(bot,item.interaction[2])
    }

    /**
     * Gets the nearest GameObject to loc with matching objectId
     * @param loc the location we are checking around
     * @param objectId the id of the object we are looking for
     * @return the nearest matching object or null.
     * @author Ceikry
     */
    fun getNearestGameObject(loc: Location, objectId: Int): GameObject? {
        var nearestObject: GameObject? = null
        val minDistance = Double.MAX_VALUE
        for (o in RegionManager.forId(loc.regionId).planes[0].objects) {
            for (obj in o) {
                if (obj != null) {
                    if (distance(loc, obj) < minDistance && obj.id == objectId) {
                        nearestObject = obj
                    }
                }
            }
        }
        return nearestObject
    }

    /**
     * Gets a list of NPCs that are attackable around the entity.
     * @param entity the entity to search around.
     * @param radius the radius around entity to search in.
     * @param name the name of the NPC to look for.
     * @return an Array of the nearest NPCs with matching name, or null.
     * @author Ceikry
     */
    private fun findTargets(entity: Entity?, radius: Int, name: String? = null): List<Entity>? {
        val targets: MutableList<Entity> = ArrayList()
        val localNPCs: Array<Any> = RegionManager.getLocalNpcs(entity, radius).toTypedArray()
        var length = localNPCs.size
        if (length > 5) {
            length = 5
        }
        for (i in 0 until length) {
            val npc = localNPCs[i] as NPC
            run { if (checkValidTargets(npc,name)) targets.add(npc) }
        }
        return if (targets.size == 0) null else targets
    }

    /**
     * Checks if the given target is a valid target for a combat bot.
     * @param target the NPC that we are checking
     * @param name the expected name of the NPC.
     * @return true if the target is valid, false if not.
     * @author Ceikry
     */
    private fun checkValidTargets(target: NPC,name: String?): Boolean {
        if (!target.isActive) {
            return false
        }
        if (!target.properties.isMultiZone && target.inCombat()) {
            return false
        }
        if (name != null){
            if(target.name != name)
                return false
        }
        return target.definition.hasAction("attack")
    }

    /**
     * Attacks npcs in the given radius of the bot.
     * @param bot the bot that is attacking
     * @param radius the radius to attack in
     * @return true if successfully attacking an NPC within that radius, false if not.
     * @author Ceikry
     */
    fun attackNpcsInRadius(bot: Player, radius: Int): Boolean {
        if (bot.inCombat()) return true
        var creatures: List<Entity>? = findTargets(bot, radius) ?: return false
        bot.attack(creatures!![RandomFunction.getRandom(creatures.size - 1)])
        return if (creatures.isNotEmpty()) {
            true
        } else {
            creatures = findTargets(bot, radius)
            if (!creatures!!.isEmpty()) {
                bot.attack(creatures[RandomFunction.getRandom(creatures.size - 1)])
                return true
            }
            false
        }
    }

    /**
     * Function to iteratively walk a bot to a faraway location. Limited by doors and large obstacles like mountains.
     * @param loc the location to walk to.
     * @author Ceikry
     */
    fun walkTo(loc: Location){
        if(!bot.walkingQueue.isMoving) {
            Executors.newSingleThreadExecutor().execute {
                walkToIterator(loc)
            }
        }
    }

    /**
     * The iterator for long-distance walking. Limited by doors and large obstacles like mountains.
     * @param loc the location to find a path to.
     * @author Ceikry
     */
    private fun walkToIterator(loc: Location){
        var diffX = loc.x - bot.location.x
        var diffY = loc.y - bot.location.y
        while(!bot.location.transform(diffX,diffY,0).withinDistance(bot.location)) {
            diffX /= 2
            diffY /= 2
        }
        GameWorld.Pulser.submit(object : MovementPulse(bot,bot.location.transform(diffX,diffY,0), Pathfinder.SMART){
            override fun pulse(): Boolean {
                return true
            }
        })
    }

    /**
     * Attacks npcs in the given radius of the bot.
     * @param bot the bot that is attacking
     * @param radius the radius to attack in
     * @param name the name of the NPC to target.
     * @return true if successfully attacking an NPC within that radius, false if not.
     * @author Ceikry
     */
    fun attackNpcInRadius(bot: Player,name: String, radius: Int): Boolean {
        if (bot.inCombat()) return true
        var creatures: List<Entity>? = findTargets(bot, radius,name) ?: return false
        bot.attack(creatures!![RandomFunction.getRandom(creatures.size - 1)])
        return if (creatures.isNotEmpty()) {
            true
        } else {
            creatures = findTargets(bot, radius,name)
            if (!creatures!!.isEmpty()) {
                bot.attack(creatures.random())
                return true
            }
            false
        }
    }

    /**
     * Extension function to find the distance between a location and a ground item.
     * @param loc the location to check the distance from.
     * @return a Double representing the distance.
     * @author Ceikry
     */
    fun GroundItem.distance(loc: Location): Double{
        return location.getDistance(loc)
    }

    /**
     * A function for teleporting the bot to the GE
     * @author Ceikry
     */
    fun teleportToGE(){
        bot.lock()
        bot.visualize(ANIMATIONUP, GRAPHICSUP)
        bot.impactHandler.disabledTicks = 4
        val location = Location.create(3165, 3482, 0)
        bot.pulseManager.run(object : Pulse(4, bot) {
            override fun pulse(): Boolean {
                bot.unlock()
                bot.properties.teleportLocation = location
                bot.animator.reset()
                return true
            }
        })
    }

    /**
     * A function for selling a given item on the GE.
     * @param id the ID of the item to sell on the GE. Pulls from the bot's bank.
     * @author Ceikry
     */
    fun sellOnGE(id: Int){
        class toCounterPulse : MovementPulse(bot,Location.create(3165, 3487, 0) ){
            override fun pulse(): Boolean {
                val itemAmt = bot.bank.getAmount(id)
                val offeredValue = checkPriceOverrides(id) ?: ItemDefinition.forId(id).value
                BotGrandExchange.sellOnGE(id, offeredValue, itemAmt)
                SystemLogger.log("Offered $itemAmt")
                bot.bank.remove(Item(id,itemAmt))
                bot.bank.refresh()
                SystemLogger.log("Banked fish: " + bot.bank.getAmount(ItemNames.RAW_LOBSTER))
                return true
            }
        }
        bot.pulseManager.run(toCounterPulse())
    }

    /**
     * Function to sell all items in a bot's bank on the Grand Exchange, if they are tradable.
     * @author Ceikry
     */
    fun sellAllOnGe(){
        class toCounterPulseAll : MovementPulse(bot,Location.create(3165, 3487, 0) ){
            override fun pulse(): Boolean {
                for(item in bot.bank.toArray()) {
                    item ?: continue
                    if (item.id == ItemNames.LOBSTER) continue
                    SystemLogger.log("Checking ${item.id}")
                    if(!item.definition.isTradeable) {continue}
                    SystemLogger.log("Adding ${item.name}")
                    val itemAmt = item.amount
                    val offeredValue = checkPriceOverrides(item.id) ?: item.definition.value
                    BotGrandExchange.sellOnGE(item.id, offeredValue, itemAmt)
                    bot.bank.remove(item)
                    bot.bank.refresh()
                }
                return true
            }
        }
        bot.pulseManager.run(toCounterPulseAll())
    }

    /**
     * Function to sell an item with given ID on the GE with the given price(value)
     * @param id the ID of the item to sell on the GE, pulls from the bot's bank.
     * @param value the price to sell the item at.
     * @author Ceikry
     */
    fun sellOnGE(id: Int, value: Int){
        class toCounterPulseWithPrice : MovementPulse(bot,Location.create(3165, 3487, 0) ){
            override fun pulse(): Boolean {
                val itemAmt = bot.bank.getAmount(id)
                val offeredValue = checkPriceOverrides(id) ?: value
                SystemLogger.log("Offered $itemAmt")
                BotGrandExchange.sellOnGE(id, offeredValue, itemAmt)
                bot.bank.remove(Item(id,itemAmt))
                bot.bank.refresh()
                SystemLogger.log("Banked fish: " + bot.bank.getAmount(ItemNames.RAW_LOBSTER))
                return true
            }
        }
        bot.pulseManager.run(toCounterPulseWithPrice())
    }

    /**
     * Function to teleport a bot to the given location.
     * @param loc the location to teleport to
     * @author Ceikry
     */
    fun teleport(loc: Location){
        bot.lock()
        bot.visualize(ANIMATIONUP, GRAPHICSUP)
        bot.impactHandler.disabledTicks = 4
        val location = loc
        GameWorld.Pulser.submit(object : Pulse(4, bot) {
            override fun pulse(): Boolean {
                bot.unlock()
                bot.properties.teleportLocation = location
                bot.animator.reset()
                return true
            }
        })
    }

    /**
     * Takes the given item out of the bot's inventory and places it into the bank. Banks all items with matching ID.
     * @param item the ID of the item to bank
     * @author Ceikry
     */
    fun bankItem(item: Int){
        class BankingPulse() : Pulse(20){
            override fun pulse(): Boolean {
                val logs = bot.inventory.getAmount(item)
                bot.inventory.remove(Item(item,logs))
                bot.bank.add(Item(item,logs))
                SystemLogger.log("${bot.username}: Banked $logs ${ItemDefinition.forId(item).name.toLowerCase()}")
                SystemLogger.log("${bot.username}: Bank currently contains ${bot.bank.getAmount(item)} ${ItemDefinition.forId(item).name.toLowerCase()}")
                return true
            }
        }
        bot.pulseManager.run(BankingPulse())
    }

    /**
     * Function that makes the bot eat the food item in their inventory if their HP is below 2/3.
     * @author Ceikry
     * @param foodId the ID of the food item to eat
     */
    fun eat(foodId: Int) {
        val foodItem = Item(foodId)
        if (bot.skills.getStaticLevel(Skills.HITPOINTS) * RandomFunction.random(0.5,0.75) >= bot.skills.lifepoints && bot.inventory.containsItem(foodItem)) {
            bot.lock(3)
            //this.animate(new Animation(829));
            val food = bot.inventory.getItem(foodItem)
            var consumable: Consumable? = Consumables.getConsumableById(foodId)
            if (consumable == null) {
                consumable = Food(intArrayOf(food.id),HealingEffect(1))
            }
            consumable.consume(food, bot)
            bot.properties.combatPulse.delayNextAttack(3)
        }
    }

    /**
     * Same as the eat function, except that it forces the bot to eat regardless of HP.
     * @author Ceikry
     * @param foodId the ID of the food item to eat.
     */
    fun forceEat(foodId: Int) {
        val foodItem = Item(foodId)
        if (bot.inventory.containsItem(foodItem)) {
            bot.lock(3)
            //this.animate(new Animation(829));
            val food = bot.inventory.getItem(foodItem)
            var consumable: Consumable? = Consumables.getConsumableById(foodId)
            if (consumable == null) {
                consumable = Food(intArrayOf(foodId),HealingEffect(1))
            }
            consumable.consume(food, bot)
            bot.properties.combatPulse.delayNextAttack(3)
        }
    }

    /**
     * Function for buying items off the GE.
     * @param itemID the id of the item to buy off the GE.
     * @param amount the amount to buy.
     * @return true if item was successfully bought, false if not.
     */
    fun buyFromGE(itemID: Int, amount: Int){
        Executors.newSingleThreadExecutor().execute{
            val offer = GrandExchangeOffer(itemID, false)
            offer.offeredValue = checkPriceOverrides(itemID) ?: ItemDefinition.forId(itemID).value
            offer.amount = amount
            GEOfferDispatch.dispatch(bot, offer)
            var bought: Boolean = false
            val latch = CountDownLatch(1)
            bot.pulseManager.run(object : Pulse(5) {
                override fun pulse(): Boolean {
                    bought = offer.completedAmount == offer.amount
                    latch.countDown()
                    return true
                }
            })
            latch.await()
            if(bought){
                bot.bank.add(Item(offer.itemId,offer.completedAmount))
                bot.bank.refresh()
            }
        }
    }

    /**
     * Method to allow a bot to withdraw items from its bank.
     * @param itemID the item to withdraw.
     * @param amount the amount to withdraw.
     * @author Ceikry
     */
    fun withdraw(itemID: Int, amount: Int){
        var item: Item? = null
        if(bot.bank.containsItem(Item(itemID,amount))){
            item = Item(itemID,amount)
        } else {
            item = Item(itemID,bot.bank.getAmount(itemID))
        }
        if(item.amount == 0) return
        if(!bot.inventory.hasSpaceFor(item)){
            item.amount = bot.inventory.getMaximumAdd(item)
        }
        bot.bank.remove(item)
        bot.inventory.add(item)
    }

    /**
     * Function to check for price overrides.
     * @param id the id to check for overrides for.
     * @author Ceikry
     */
    fun checkPriceOverrides(id: Int): Int?{
        return when(id){
            ItemNames.DRAGON_BONES ->          1250
            ItemNames.GREEN_DRAGONHIDE_1753 -> 550
            ItemNames.BOW_STRING_1777 ->       250
            ItemNames.MAGIC_LOGS_1513 ->       450
            ItemNames.GRIMY_RANARR ->          1214
            ItemNames.GRIMY_AVANTOE ->         453
            ItemNames.GRIMY_CADANTINE ->       232
            ItemNames.GRIMY_DWARF_WEED ->      86
            ItemNames.GRIMY_GUAM ->            50
            ItemNames.GRIMY_HARRALANDER ->     115
            ItemNames.GRIMY_IRIT ->            860
            ItemNames.GRIMY_KWUARM ->          334
            ItemNames.GRIMY_LANTADYME ->       115
            ItemNames.GRIMY_MARRENTILL ->      250
            ItemNames.LOBSTER ->               268
            ItemNames.LOOP_HALF_OF_KEY ->      5250
            ItemNames.TOOTH_HALF_OF_KEY ->     4263
            else -> null
        }
    }
}