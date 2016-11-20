package com.tyler.tetris;

import static com.tyler.tetris.BlockType.BOX;
import static com.tyler.tetris.BlockType.DIAMOND;
import static com.tyler.tetris.BlockType.L_BLOCK_L;
import static com.tyler.tetris.BlockType.L_BLOCK_R;
import static com.tyler.tetris.BlockType.ROCKET;
import static com.tyler.tetris.BlockType.STRAIGHT_LINE;
import static com.tyler.tetris.BlockType.S_BLOCK_L;
import static com.tyler.tetris.BlockType.S_BLOCK_R;
import static com.tyler.tetris.BlockType.TWIN_PILLARS;
import static com.tyler.tetris.BlockType.T_BLOCK;

import java.util.HashMap;
import java.util.Map;

public enum Difficulty {
	
	EASY {{
		
		linesPerLevel = 15;
		initialTimerDelay = 650;
		timerSpeedup = 40;
		timeAttackBonus = 150;
		timeAttackSecondsPerLine = 5;
		winBonus = 500;
		
		type_spawn.put(BOX, 14);
		type_spawn.put(L_BLOCK_L, 14);
		type_spawn.put(L_BLOCK_R, 14);
		type_spawn.put(S_BLOCK_L, 14);
		type_spawn.put(S_BLOCK_R, 14);
		type_spawn.put(STRAIGHT_LINE, 14);
		type_spawn.put(T_BLOCK, 14);
		type_spawn.put(TWIN_PILLARS, 10);
		type_spawn.put(ROCKET, 8);
		type_spawn.put(DIAMOND, 5);
		
	}},
	
	MEDIUM {{
		
		linesPerLevel = 20;
		initialTimerDelay = 600;
		timerSpeedup = 45;
		timeAttackBonus = 175;
		timeAttackSecondsPerLine = 4;
		winBonus = 750;
		
		type_spawn.put(BOX, 13);
		type_spawn.put(L_BLOCK_L, 15);
		type_spawn.put(L_BLOCK_R, 14);
		type_spawn.put(S_BLOCK_L, 15);
		type_spawn.put(S_BLOCK_R, 15);
		type_spawn.put(STRAIGHT_LINE, 12);
		type_spawn.put(T_BLOCK, 14);
		type_spawn.put(TWIN_PILLARS, 10);
		type_spawn.put(ROCKET, 8);
		type_spawn.put(DIAMOND, 5);
		
	}},
	
	HARD {{
		
		linesPerLevel = 25;
		initialTimerDelay = 560;
		timerSpeedup = 55;
		timeAttackBonus = 200;
		timeAttackSecondsPerLine = 3;
		winBonus = 1000;
		
		type_spawn.put(BOX, 13);
		type_spawn.put(L_BLOCK_L, 15);
		type_spawn.put(L_BLOCK_R, 16);
		type_spawn.put(S_BLOCK_L, 16);
		type_spawn.put(S_BLOCK_R, 16);
		type_spawn.put(STRAIGHT_LINE, 11);
		type_spawn.put(T_BLOCK, 13);
		type_spawn.put(TWIN_PILLARS, 11);
		type_spawn.put(ROCKET, 8);
		type_spawn.put(DIAMOND, 6);
		
	}};

	protected int linesPerLevel;
	protected int initialTimerDelay;
	protected int timerSpeedup;
	protected int timeAttackBonus;
	protected int timeAttackSecondsPerLine;
	protected int winBonus;
	protected int linesClearedBonus;
	protected Map<BlockType, Integer> type_spawn = new HashMap<>();
	
	public int getLinesPerLevel() {
		return linesPerLevel;
	}
	
	public int getInitialTimerDelay() {
		return initialTimerDelay;
	}
	
	public int getTimerSpeedup() {
		return timerSpeedup;
	}
	
	public int getTimeAttackBonus() {
		return timeAttackBonus;
	}
	
	public int getTimeAttackSecondsPerLine() {
		return timeAttackSecondsPerLine;
	}
	
	public int getWinBonus() {
		return winBonus;
	}
	
	public int getLinesClearedBonus() {
		return linesClearedBonus;
	}
	
	public int getSpawnRate(BlockType type) {
		return type_spawn.get(type);
	}
	
	public String toString() {
		return name().charAt(0) + name().substring(1).toLowerCase();
	}
	
	
}
