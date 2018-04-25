package com.jadventure.game.menus;

import com.jadventure.game.DeathException;
import com.jadventure.game.entities.Entity;
import com.jadventure.game.entities.Player;
import com.jadventure.game.entities.NPC;
import com.jadventure.game.monsters.Monster;
import com.jadventure.game.QueueProvider;
import com.jadventure.game.CharacterChange;
import com.jadventure.game.items.ItemStack;
import com.jadventure.game.items.Item;
import com.jadventure.game.GameBeans;

import java.util.Random;
import java.util.List;
import java.util.ArrayList;

public class BattleMenu extends Menus {

    private Monster monsterOpponent;
    private NPC npcOpponent;
    private Entity opponent;
    private Player player;
    private Random random;
    private int armour;
    private double damage;
    private int escapeSuccessfulAttempts = 0;
    
    private BattleMenu(Entity opponent, Player player) throws DeathException {
    	this.random = new Random();
        this.opponent = opponent;
        this.player = player;
        this.buildBattleMenu();
        this.armour = player.getArmour();
        this.damage = player.getDamage();
    }
    
    public BattleMenu(NPC npcOpponent, Player player) throws DeathException {
    	this((Entity)npcOpponent, player);
        this.npcOpponent = npcOpponent;
        this.getBattleChoice();
    }

    public BattleMenu(Monster monsterOpponent, Player player) throws DeathException {
    	this((Entity)monsterOpponent, player);
        this.monsterOpponent = monsterOpponent;
        this.getBattleChoice();
    }

    public void buildBattleMenu() {
        this.menuItems.add(new MenuItem("Attack", "Attack " + opponent.getName() + "."));
        this.menuItems.add(new MenuItem("Defend", "Defend against " + opponent.getName() + "'s attack."));
        this.menuItems.add(new MenuItem("Escape", "Try and escape from " + opponent.getName()));
        this.menuItems.add(new MenuItem("Equip", "Equip an item"));
        this.menuItems.add(new MenuItem("Unequip", "Unequip an item"));
        this.menuItems.add(new MenuItem("View", "View details about your character"));
    }
    
    private void getBattleChoice() throws DeathException {
    	while (opponent.getHealth() > 0 && player.getHealth() > 0 && (escapeSuccessfulAttempts <= 0)) {
            QueueProvider.offer("\nWhat is your choice?");
            MenuItem selectedItem = displayMenu(this.menuItems);
            
            testSelected(selectedItem);
        }
        if (player.getHealth() == 0) {
            playerDeath();
        }  else if (opponent.getHealth() == 0) {
            opponentDeath();
        }
    }
    
    private void attack() {
    	mutateStats(0.5, 1);
    	attack(player, opponent);
    	attack(opponent, player);
    	resetStats();
    }
    
    private void defend() {
    	mutateStats(0.5, 1);
        QueueProvider.offer("\nYou get ready to defend against the " + opponent.getName() + ".");
        attack(player, opponent);
        attack(opponent, player);
        resetStats();
    }
    

    private void testSelected(MenuItem m) {
        switch (m.getKey()) {
            case "attack": {
                attack();
                break;
            }
            case "defend": {
                defend();
                break;
            }
            case "escape": {
                escapeSuccessfulAttempts = escapeAttempt(player, opponent, escapeSuccessfulAttempts);
                break;
            }
            case "equip": {
                equip();
                break;
            }
            case "unequip": {
                unequip();
                break;
            }
            case "view": {
                viewStats();
                break;
            }
            default: {
                break;
            }
        }
    }

    private int escapeAttempt(Player player, Entity attacker, int escapeAttempts) {
        if (escapeAttempts == -10)
        {
        	escapeAttempts = 0;
        }
        double playerEscapeLevel = player.getIntelligence() + player.getStealth() + player.getDexterity();
        double attackerEscapeLevel = attacker.getIntelligence() + attacker.getStealth() + attacker.getDexterity() + 
            (attacker.getDamage() / playerEscapeLevel);
        double escapeLevel = playerEscapeLevel / attackerEscapeLevel;

        Random rand = new Random();
        int rawLuck = rand.nextInt(player.getLuck()*2) + 1;
        int lowerBound = 60 - rawLuck;
        int upperBound = 80 - rawLuck;
        double minEscapeLevel = (rand.nextInt((upperBound - lowerBound) + 1) + lowerBound) / 100.0;
      
        if (escapeLevel > minEscapeLevel && (escapeAttempts == 0)) {
            QueueProvider.offer("You have managed to escape the: " + attacker.getName());
            return 1;
	} else if (escapeAttempts < 0)
	{
            QueueProvider.offer("You have tried to escape too many times!");
	    return escapeAttempts-1;
	    
        } else {
            QueueProvider.offer("You failed to escape the: " + attacker.getName());
            return escapeAttempts-1;
        }
    }

    private void attack(Entity attacker, Entity defender) {
        if (attacker.getHealth() == 0) {
            return;
        }
        double damage = attacker.getDamage();
        double critCalc = random.nextDouble();
        if (critCalc < attacker.getCritChance()) {
            damage += damage;
            QueueProvider.offer("Crit hit! Damage has been doubled!");
        }
        int healthReduction = (int) ((((3 * attacker.getLevel() / 50 + 2) * damage * damage / (defender.getArmour() + 1)/ 100) + 2) * (random.nextDouble() + 1));
        defender.setHealth((defender.getHealth() - healthReduction));
        if (defender.getHealth() < 0) {
            defender.setHealth(0);
        }
        QueueProvider.offer(healthReduction + " damage dealt!");
        if (attacker instanceof Player) {
            QueueProvider.offer("The " + defender.getName() + "'s health is " + defender.getHealth());
        } else {
            QueueProvider.offer("Your health is " + defender.getHealth());
        }
    }

    private void mutateStats(double damageMult, double armourMult) {
        armour = player.getArmour();
        damage = player.getDamage();
        player.setArmour((int) (armour * armourMult));
        player.setDamage(damage * damageMult);
    }

    private void resetStats() {
        player.setArmour(armour);
        player.setDamage(damage);
    }

    private void equip() {
        player.printStorage();
        QueueProvider.offer("What item do you want to use?");
        String itemName = QueueProvider.take();
        if (!itemName.equalsIgnoreCase("back")) {
            player.equipItem(itemName);
        }
    }

    private void unequip() {
        player.printEquipment();
        QueueProvider.offer("What item do you want to unequip?");
        String itemName = QueueProvider.take();
        if (!itemName.equalsIgnoreCase("back")) {
            player.dequipItem(itemName);
        }
    }

    private void viewStats() {
        QueueProvider.offer("\nWhat is your command? ex. View stats(vs), View Backpack(vb), View Equipment(ve) ");
        String input = QueueProvider.take();
        switch (input) {
            case "vs":
            case "viewstats":
                player.getStats();
                break;
            case "ve":
            case "viewequipped":
                player.printEquipment();
                break;
            case "vb":
            case "viewbackpack":
                player.printStorage();
                break;
            case "back":
            case "exit":
                break;
            default:
                viewStats();
                break;
        }
    }
    
    public void playerDeath() throws DeathException {
    	QueueProvider.offer("You died... Start again? (y/n)");
        String reply = QueueProvider.take().toLowerCase();
        while (!reply.startsWith("y") && !reply.startsWith("n")) {
            QueueProvider.offer("You died... Start again? (y/n)");
            reply = QueueProvider.take().toLowerCase();
        }
        if (reply.startsWith("y")) {
            throw new DeathException("restart");
        } else if (reply.startsWith("n")) {
            throw new DeathException("close");
        }
    }
    
    public void opponentDeath() {
    	int xp;
    	if (npcOpponent == null) {
    		xp = ((Monster)opponent).getXPGain();
    	} else {
    		xp = ((NPC)opponent).getXPGain();
    	}
    	
        this.player.setXP(this.player.getXP() + xp);
        int oldLevel = this.player.getLevel();
        int newLevel = (int) (0.075 * Math.sqrt(this.player.getXP()) + 1);
        this.player.setLevel(newLevel);
        
        opponentLoot();

        if (npcOpponent == null) {
        	this.player.getLocation().removeMonster((Monster)opponent);
    	} else {
    		this.player.getLocation().removeNpc((NPC)opponent);
    	}
        
        this.player.setGold(this.player.getGold() + opponent.getGold());
        QueueProvider.offer("You killed a " + opponent.getName() + "\nYou have gained " + xp + " XP and " + opponent.getGold() + " gold");
        if (oldLevel < newLevel) {
            QueueProvider.offer("You've are now level " + newLevel + "!");
        }
        CharacterChange cc = new CharacterChange();
        cc.trigger(this.player, "kill", opponent.getName());
    }
    
    public void opponentLoot() {
        List<String> itemIds = new ArrayList<>();
        for (ItemStack itemStack : opponent.getStorage().getItemStack()) {
            itemIds.add(itemStack.getItem().getId());
        }
        dropLoot(itemIds);
    }
    
    public void dropLoot(List<String> itemIds) {
    	for (String itemId : itemIds) {
            Item item = GameBeans.getItemRepository().getItem(itemId);
            opponent.removeItemFromStorage(item);
            this.player.getLocation().addItem(item);
            QueueProvider.offer("Your opponent dropped a " + item.getName());
        }
    }
}

