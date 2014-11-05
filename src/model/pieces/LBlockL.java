package model.pieces;


import java.awt.Color;

import ui.GameBoardPanel;

// LBLockL:
// ..X.
// ..X.
// .XX.
public class LBlockL extends AbstractPiece {
	
	private static final int START_ROW = 2;
	
	private static final int[][][] orientationMap = {
		
		// Standard orientation:
		// X..
		// XXX
		// ...
		{
			{-2,0},
			{-1,0},
			{-1,1},
			{-1,2}
		},
		
		// North orientation:
		// .XX
		// .X.
		// .X.
		{
			{-2,1},
			{-2,2},
			{-1,1},
			{0,1}
		},
		
		// East orientation:
		// ...
		// XXX
		// ..X
		{
			{-1,0},
			{-1,1},
			{-1,2},
			{0,2}
		},
		
		// South orientation:
		// .X.
		// .X.
		// XX.
		{
			{0,0},
			{0,1},
			{-1,1},
			{-2,1}
		},
		
	};
	
	private final static int[][] nextPanelSquares = {
		{1,1},{2,1},{2,2},{2,3}
	};
	
	private static final int[][] initialSquares = {
		{START_ROW, GameBoardPanel.CENTER_OFFSET},
		{START_ROW, GameBoardPanel.CENTER_OFFSET+1},
		{START_ROW, GameBoardPanel.CENTER_OFFSET+2}
	};
	
	public LBlockL(Color color) { super(color, START_ROW); }

	public int[][] getNextPanelSquares() { return nextPanelSquares; }

	public int[][][] getOrientationMap() { return orientationMap; }
	
}