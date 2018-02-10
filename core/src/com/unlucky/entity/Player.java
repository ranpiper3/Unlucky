package com.unlucky.entity;

import com.badlogic.gdx.graphics.g2d.SpriteBatch;
import com.badlogic.gdx.math.Vector2;
import com.badlogic.gdx.utils.Array;
import com.unlucky.animation.AnimationManager;
import com.unlucky.battle.Moveset;
import com.unlucky.inventory.Equipment;
import com.unlucky.inventory.Inventory;
import com.unlucky.inventory.Item;
import com.unlucky.map.Tile;
import com.unlucky.map.TileMap;
import com.unlucky.resource.ResourceManager;
import com.unlucky.resource.Util;

/**
 * The protagonist of the game.
 *
 * @author Ming Li
 */
public class Player extends Entity {

    // Battle
    private Enemy opponent;
    private boolean battling = false;

    // exp and level up
    private int exp;
    private int maxExp;

    private int hpIncrease = 0;
    private int minDmgIncrease = 0;
    private int maxDmgIncrease = 0;
    private int accuracyIncrease = 0;
    private int maxExpIncrease = 0;

    // gold
    private int gold = 0;

    // inventory and equips
    public Inventory inventory;
    public Equipment equips;

    public Player(String id, ResourceManager rm) {
        super(id, rm);

        inventory = new Inventory();
        equips = new Equipment();

        // attributes
        hp = maxHp = previousHp = Util.PLAYER_INIT_MAX_HP;
        accuracy = Util.PLAYER_ACCURACY;
        minDamage = Util.PLAYER_INIT_MIN_DMG;
        maxDamage = Util.PLAYER_INIT_MAX_DMG;

        level = 1;
        speed = 50.f;

        exp = 0;
        // offset between 3 and 5
        maxExp = Util.calculateMaxExp(1, Util.getRandomValue(3, 5, rand));

        // create tilemap animation
        am = new AnimationManager(rm.sprites16x16, Util.PLAYER_WALKING, Util.PLAYER_WALKING_DELAY);
        // create battle scene animation
        bam = new AnimationManager(rm.battleSprites96x96, 2, Util.PLAYER_WALKING, 2 / 5f);

        moveset = new Moveset(rm);
        // damage seed is a random number between the damage range
        moveset.reset(minDamage, maxDamage, maxHp);
    }

    public Player(String id, Vector2 position, TileMap tileMap, ResourceManager rm) {
        super(id, position, tileMap, rm);

        inventory = new Inventory();
        equips = new Equipment();

        // attributes
        hp = maxHp = previousHp = Util.PLAYER_INIT_MAX_HP;
        accuracy = Util.PLAYER_ACCURACY;
        minDamage = Util.PLAYER_INIT_MIN_DMG;
        maxDamage = Util.PLAYER_INIT_MAX_DMG;

        level = 1;
        speed = 50.f;

        exp = 0;
        // offset between 3 and 5
        maxExp = Util.calculateMaxExp(1, Util.getRandomValue(3, 5, rand));

        // create tilemap animation
        am = new AnimationManager(rm.sprites16x16, Util.PLAYER_WALKING, Util.PLAYER_WALKING_DELAY);
        // create battle scene animation
        bam = new AnimationManager(rm.battleSprites96x96, 2, Util.PLAYER_WALKING, 2 / 5f);

        moveset = new Moveset(rm);
        // damage seed is a random number between the damage range
        moveset.reset(minDamage, maxDamage, maxHp);
    }

    public void update(float dt) {
        super.update(dt);

        // Stop animation when player isn't moving
        if (canMove()) am.stopAnimation();

        // check for Entity interaction
        if (tileMap.containsEntity(tileMap.toTileCoords(position)) && canMove()) {
            opponent = (Enemy) tileMap.getEntity(tileMap.toTileCoords(position));
            battling = true;
        }
    }

    public void render(SpriteBatch batch) {
        // draw shadow
        batch.draw(rm.shadow11x6, position.x + 3, position.y - 3);
        batch.draw(am.getKeyFrame(true), position.x + 1, position.y);
    }

    /**
     * Increments level and recalculates max exp
     * Sets increase variables to display on screen
     * Recursively accounts for n consecutive level ups from remaining exp
     *
     * @param remainder the amount of exp left after a level up
     */
    public void levelUp(int remainder) {
        level++;

        hpIncrease += Util.getRandomValue(Util.PLAYER_MIN_HP_INCREASE, Util.PLAYER_MAX_HP_INCREASE, rand);
        int dmgMean = Util.getRandomValue(Util.PLAYER_MIN_DMG_INCREASE, Util.PLAYER_MAX_DMG_INCREASE, rand);

        // deviates from mean by 0 to 2
        minDmgIncrease += (dmgMean - rand.nextInt(2));
        maxDmgIncrease += (dmgMean + rand.nextInt(2));
        // accuracy increases by 1% every 10 levels
        accuracyIncrease += level % 10 == 0 ? 1 : 0;

        int prevMaxExp = maxExp;
        maxExp = Util.calculateMaxExp(level, Util.getRandomValue(3, 5, rand));
        maxExpIncrease += (maxExp - prevMaxExp);

        // another level up
        if (remainder >= maxExp) {
            levelUp(remainder - maxExp);
        } else {
            exp = remainder;
        }
    }

    /**
     * Increases the actual stats by their level up amounts
     */
    public void applyLevelUp() {
        maxHp += hpIncrease;
        hp = maxHp;
        minDamage += minDmgIncrease;
        maxDamage += maxDmgIncrease;
        accuracy += accuracyIncrease;

        // reset variables
        hpIncrease = 0;
        minDmgIncrease = 0;
        maxDmgIncrease = 0;
        accuracyIncrease = 0;
        maxExpIncrease = 0;
    }

    /**
     * Applies the stats of an equipable item
     *
     * @param item
     */
    public void equip(Item item) {
        maxHp += item.mhp;
        minDamage += item.dmg;
        maxDamage += item.dmg;
        accuracy += item.acc;
        if (hp > maxHp) hp = maxHp;
    }

    /**
     * Removes the stats of an equipable item
     *
     * @param item
     */
    public void unequip(Item item) {
        maxHp -= item.mhp;
        minDamage -= item.dmg;
        maxDamage -= item.dmg;
        accuracy -= item.acc;
        if (hp > maxHp) hp = maxHp;
    }

    public Enemy getOpponent() {
        return opponent;
    }

    public void finishBattling() {
        battling = false;
        opponent = null;
    }

    public void finishTileInteraction() {
        tileInteraction = false;
    }

    /**
     * After teleportation is done the player is moved out of the tile in a random direction
     */
    public void finishTeleporting() {
        teleporting = false;
        changeDirection(rand.nextInt(4));
    }

    public void potion(int heal) {
        hp += heal;
        if (hp > maxHp) hp = maxHp;
    }

    /**
     * Green question mark tiles can drop 70% of the time
     * if does drop:
     * - gold (50% of the time) (based on map level)
     * - heals for 20% of max hp (45% of the time)
     * - items (5% of the time)
     *
     * @return
     */
    public String[] getQuestionMarkDialog(int mapLevel) {
        String[] ret = null;

        if (Util.isSuccess(Util.TILE_INTERATION, rand)) {
            int k = rand.nextInt(100);
            // gold
            if (k < 50) {
                // gold per level scaled off map's average level
                int gold = 0;
                for (int i = 0; i < mapLevel; i++) {
                    gold += Util.getRandomValue(7, 13, rand);
                }
                this.gold += gold;
                ret = new String[] {
                        "The random tile gave something!",
                        "You obtained " + gold + " gold!"
                };
            }
            // heal
            else if (k < 95) {
                int heal = (int) (0.2 * maxHp);
                this.hp += heal;
                if (hp > maxHp) hp = maxHp;
                ret = new String[] {
                        "The random tile gave something!",
                        "It healed you for " + heal + " hp!"
                };
            }
            // item
            else if (k < 100) {
                Item item = rm.getRandomItem(rand);
                if (inventory.isFull()) {
                    ret = new String[] {
                            "The random tile gave something!",
                            "It dropped a " + item.getDialogName() + "!",
                            "Oh no, too bad your inventory was full."
                    };
                }
                else {
                    ret = new String[]{
                            "The random tile gave something!",
                            "It dropped a " + item.getDialogName() + "!",
                            "The item was added to your inventory."
                    };
                    item.adjust(mapLevel, rand);
                    inventory.addItem(item);
                }
            }
        }
        else {
            ret = new String[] {
                    "The random tile did not give anything."
            };
        }

        return ret;
    }

    /**
     * The purple exclamation mark tile is a destructive tile
     * that has a 60% chance to do damage to the player and
     * 40% chance to steal gold.
     *
     * @param mapLevel
     * @return
     */
    public String[] getExclamDialog(int mapLevel) {
        String[] ret = null;

        if (Util.isSuccess(Util.TILE_INTERATION, rand)) {
            if (Util.isSuccess(60, rand)) {
                int dmg = 3 * mapLevel;
                hp -= dmg;
                if (hp <= 0) {
                    ret = new String[]{"" +
                            "The random tile cursed you!",
                            "It damaged you for " + dmg + " damage!",
                            "Shit you actually died omegalul."
                    };
                    hp = maxHp;
                }
                else {
                    ret = new String[]{
                            "The random tile cursed you!",
                            "It damaged you for " + dmg + " damage!"
                    };
                }
            }
            else {
                int steal = 0;
                for (int i = 0; i < mapLevel; i++) {
                    steal += Util.getRandomValue(4, 9, rand);
                }
                gold -= steal;
                if (gold < 0) gold = 0;
                ret = new String[] {
                        "The random tile cursed you!",
                        "It caused you to lose " + steal + " gold!"
                };
            }
        }
        else {
            ret = new String[] {
                "The random tile did not affect you."
            };
        }

        return ret;
    }

    /**
     * Sets the player's position to another teleportation tile anywhere on the map
     */
    public void teleport() {
        Tile currentTile = tileMap.getTile(tileMap.toTileCoords(position));
        Array<Tile> candidates = tileMap.getTeleportationTiles(currentTile);
        Tile choose = candidates.get(rand.nextInt(candidates.size));
        position.set(tileMap.toMapCoords(choose.tilePosition));
    }

    public boolean isBattling() {
        return battling;
    }

    public void setExp(int exp) {
        this.exp = exp;
    }

    public void addExp(int exp) {
        this.exp += exp;
    }

    public void setMaxExp(int maxExp) {
        this.maxExp = maxExp;
    }

    public int getExp() {
        return exp;
    }

    public int getMaxExp() {
        return maxExp;
    }

    public int getHpIncrease() {
        return hpIncrease;
    }

    public void setHpIncrease(int hpIncrease) {
        this.hpIncrease = hpIncrease;
    }

    public int getMinDmgIncrease() {
        return minDmgIncrease;
    }

    public void setMinDmgIncrease(int minDmgIncrease) {
        this.minDmgIncrease = minDmgIncrease;
    }

    public int getMaxDmgIncrease() {
        return maxDmgIncrease;
    }

    public void setMaxDmgIncrease(int maxDmgIncrease) {
        this.maxDmgIncrease = maxDmgIncrease;
    }

    public int getAccuracyIncrease() {
        return accuracyIncrease;
    }

    public void setAccuracyIncrease(int accuracyIncrease) {
        this.accuracyIncrease = accuracyIncrease;
    }

    public int getMaxExpIncrease() { return maxExpIncrease; }

    public void addGold(int g) { this.gold += g; }

    public int getGold() { return gold; }

}