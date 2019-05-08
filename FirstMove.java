package player;
import java.util.function.Function;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PlayerInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class FirstMove {
	public Action firstmove;
	public Action secondmove;
	public Action thirdmove;
	public int puyoa1;
	public int puyoa2;
	public int puyob1;
	public int puyob2;
	public int puyoc1;
	public int puyoc2;
	// RIGHTで0~4, LEFTで1~5に置ける
	// PuyoDirection は、FirstPuyoに対するSecondPuyoの相対的な方向を表している。
	public Function<Board, Action> ActionAfterNexNexNexSeen;
	
	public FirstMove(Board myboard) {
		Puyo pa = myboard.getCurrentPuyo();
		Puyo pb = myboard.getNextPuyo();
		Puyo pc = myboard.getNextNextPuyo();
		PuyoType pa1 = pa.getPuyoType(PuyoNumber.FIRST);
		PuyoType pa2 = pa.getPuyoType(PuyoNumber.SECOND);
		PuyoType pb1 = pb.getPuyoType(PuyoNumber.FIRST);
		PuyoType pb2 = pb.getPuyoType(PuyoNumber.SECOND);
		PuyoType pc1 = pc.getPuyoType(PuyoNumber.FIRST);
		PuyoType pc2 = pc.getPuyoType(PuyoNumber.SECOND);
		if (pa1 == pa2) {
			if (pb1 == pb2) {
				if (pa1 == pb1) {
					// 最初の4色すべて同じ色だった場合
					// 隣に置いて全消しをする
					firstmove = new Action(PuyoDirection.DOWN, 2);
					secondmove = new Action(PuyoDirection.DOWN, 3);
				}
				else {
					// 上下同色のぷよが2つ来た場合（赤赤・黄黄など）
					if (pc1 == pa1) {
						firstmove = new Action(PuyoDirection.RIGHT, 0);
						secondmove = new Action(PuyoDirection.RIGHT, 0);
						if (pc2 == pa1) {
							// 赤赤・黄黄・赤赤
							thirdmove = new Action(PuyoDirection.DOWN, 0);
						}
						else if (pc2 == pb1) {
							// 赤赤・黄黄・赤黄
							thirdmove = new Action(PuyoDirection.UP, 2);
						}
						else {
							// 赤赤・黄黄・赤緑
							thirdmove = new Action(PuyoDirection.DOWN, 2);
						}
					}
					else if (pc2 == pa1) {
						firstmove = new Action(PuyoDirection.RIGHT, 0);
						secondmove = new Action(PuyoDirection.RIGHT, 0);
						if (pc1 == pb1) {
							// 赤赤・黄黄・黄赤
							thirdmove = new Action(PuyoDirection.DOWN, 2);
						}
						else {
							// 赤赤・黄黄・緑赤
							thirdmove = new Action(PuyoDirection.UP, 2);
						}
					}
					else if (pc1 == pb1) {
						firstmove = new Action(PuyoDirection.RIGHT, 0);
						if (pc1 == pc2) {
							// 赤赤・黄黄・黄黄
							secondmove = new Action(PuyoDirection.RIGHT, 2);
							ActionAfterNexNexNexSeen = board -> {
								PuyoType pt1 = board.getNextNextPuyo().getPuyoType(PuyoNumber.FIRST);
								PuyoType pt2 = board.getNextNextPuyo().getPuyoType(PuyoNumber.SECOND);
								if (pt1 == pt2 && pt1 == pa1) {
									return new Action(PuyoDirection.DOWN, 1);
								}
								else {
									return new Action(PuyoDirection.RIGHT, 0);
								}
							};
						}
						else {
							// 赤赤・黄黄・黄緑
							secondmove = new Action(PuyoDirection.RIGHT, 0);
							thirdmove = new Action(PuyoDirection.UP, 0);
						}
					}
					else if (pc2 == pb1) {
						// 赤赤・黄黄・緑黄
						firstmove = new Action(PuyoDirection.RIGHT, 0);
						secondmove = new Action(PuyoDirection.RIGHT, 0);
						thirdmove = new Action(PuyoDirection.DOWN, 0);
					}
					else {
						// 赤赤・黄黄・緑緑
						// 赤赤・黄黄・緑青
						// 赤赤・黄黄・青緑
						firstmove = new Action(PuyoDirection.RIGHT, 0);
						secondmove = new Action(PuyoDirection.RIGHT, 0);
					}
				}
			}
			else{
				firstmove = new Action(PuyoDirection.DOWN, 0);
				if (pb1 == pa1) {
					// 赤赤・赤黄
					secondmove = new Action(PuyoDirection.UP, 1);
					if (pc1 == pc2) {
						if (pc1 != pa1) {
							// 赤赤・赤黄・黄黄
							// 赤赤・赤黄・緑緑
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
						else {
							// 赤赤・赤黄・赤赤
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
					}
					else if (pc1 == pa1) {
						if (pc2 == pb2) {
							// 赤赤・赤黄・赤黄
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else {
							// 赤赤・赤黄・赤緑
							thirdmove = new Action(PuyoDirection.LEFT, 1);
						}
					}
					else if (pc2 == pa1) {
						if (pc1 == pb2) {
							// 赤赤・赤黄・黄赤
							thirdmove = new Action(PuyoDirection.UP, 1);
						}
						else {
							// 赤赤・赤黄・緑赤
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
					}
					else {
						// 赤赤・赤黄・黄緑
						// 赤赤・赤黄・緑黄
						// 赤赤・赤黄・緑青
						thirdmove = null;
					}
				}
				else if (pb2 == pa1) {
					// 赤赤・黄赤
					secondmove = new Action(PuyoDirection.DOWN, 1);
					if (pc1 == pc2) {
						if (pc1 != pa1) {
							// 赤赤・黄赤・黄黄
							// 赤赤・黄赤・緑緑
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
						else {
							// 赤赤・黄赤・赤赤
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
					}
					else if (pc1 == pa1) {
						if (pc2 == pb1) {
							// 赤赤・黄赤・赤黄
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else {
							// 赤赤・黄赤・赤緑
							thirdmove = new Action(PuyoDirection.LEFT, 1);
						}
					}
					else if (pc2 == pa1) {
						if (pc1 == pb1) {
							// 赤赤・黄赤・黄赤
							thirdmove = new Action(PuyoDirection.UP, 1);
						}
						else {
							// 赤赤・黄赤・緑赤
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
					}
					else {
						// 赤赤・黄赤・黄緑
						// 赤赤・黄赤・緑黄
						// 赤赤・黄赤・緑青
						thirdmove = null;
					}
				}
				else {
					// 赤赤・黄緑
					if (pc1 == pa1) {
						if (pc1 == pc2) {
							// 赤赤・黄緑・赤赤
							secondmove = new Action(PuyoDirection.DOWN, 1);
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else if (pc2 == pb1) {
							// 赤赤・黄緑・赤黄
							secondmove = new Action(PuyoDirection.RIGHT, 2);
							thirdmove = new Action(PuyoDirection.UP, 1);
						}
						else if (pc2 == pb2) {
							// 赤赤・黄緑・赤緑
							secondmove = new Action(PuyoDirection.LEFT, 3);
							thirdmove = new Action(PuyoDirection.UP, 1);
						}
						else {
							// 赤赤・黄緑・赤青
							secondmove = new Action(PuyoDirection.DOWN, 1);
							thirdmove = new Action(PuyoDirection.LEFT, 1);
						}
					}
					else if (pc2 == pa1) {
						if (pc1 == pb1) {
							// 赤赤・黄緑・黄赤
							secondmove = new Action(PuyoDirection.RIGHT, 2);
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else if (pc1 == pb2) {
							// 赤赤・黄緑・緑赤
							secondmove = new Action(PuyoDirection.LEFT, 3);
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else {
							// 赤赤・黄緑・青赤
							secondmove = new Action(PuyoDirection.DOWN, 1);
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
					}
					else {
						if (pc1 == pb1) {
							if (pc1 == pc2) {
								// 赤赤・黄緑・黄黄
								secondmove = new Action(PuyoDirection.DOWN, 1);
								thirdmove = new Action(PuyoDirection.RIGHT, 0);
							}
							else if (pc2 == pb2) {
								// 赤赤・黄緑・黄緑
								secondmove = new Action(PuyoDirection.DOWN, 1);
								thirdmove = new Action(PuyoDirection.DOWN, 2);
							}
							else {
								// 赤赤・黄緑・黄青
								secondmove = new Action(PuyoDirection.UP, 1);
								thirdmove = new Action(PuyoDirection.RIGHT, 2);
							}
						}
						else if (pc2 == pb1) {
							if (pc1 == pb2) {
								// 赤赤・黄緑・緑黄
								secondmove = new Action(PuyoDirection.DOWN, 1);
								thirdmove = new Action(PuyoDirection.UP, 2);
							}
							else {
								// 赤赤・黄緑・青黄
								secondmove = new Action(PuyoDirection.UP, 1);
								thirdmove = new Action(PuyoDirection.LEFT, 3);
							}
						}
						else if (pc1 == pb2) {
							if (pc1 == pc2) {
								// 赤赤・黄緑・緑緑
								secondmove = new Action(PuyoDirection.UP, 1);
								thirdmove = new Action(PuyoDirection.RIGHT, 0);
							}
							else {
								// 赤赤・黄緑・緑青
								secondmove = new Action(PuyoDirection.DOWN, 1);
								thirdmove = new Action(PuyoDirection.RIGHT, 2);
							}
						}
						else if (pc2 == pb2) {
							// 赤赤・黄緑・青緑
							secondmove = new Action(PuyoDirection.DOWN, 1);
							thirdmove = new Action(PuyoDirection.LEFT, 3);
						}
						else {
							if (pc1 == pc2) {
								// 赤赤・黄緑・青青
							}
							else {
								// 赤赤・黄緑・青紫
							}
						}
					}
				}
			}
		}
		else {
			// 赤黄
			if (pb1 == pb2) {
				if (pb1 == pa1) {
					// 赤黄・赤赤
					firstmove = new Action(PuyoDirection.UP, 1);
					secondmove = new Action(PuyoDirection.DOWN, 0);
					if (pc1 == pc2) {
						if (pc1 != pb1) {
							// 赤黄・赤赤・黄黄
							// 赤黄・赤赤・緑緑
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
						else {
							// 赤黄・赤赤・赤赤
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
					}
					else if (pc1 == pb1) {
						if (pc2 == pa2) {
							// 赤黄・赤赤・赤黄
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else {
							// 赤黄・赤赤・赤緑
							thirdmove = new Action(PuyoDirection.LEFT, 1);
						}
					}
					else if (pc2 == pb1) {
						if (pc1 == pa2) {
							// 赤黄・赤赤・黄赤
							thirdmove = new Action(PuyoDirection.UP, 1);
						}
						else {
							// 赤黄・赤赤・緑赤
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
					}
					else {
						// 赤黄・赤赤・黄緑
						// 赤黄・赤赤・緑黄
						// 赤黄・赤赤・緑青
						thirdmove = null;
					}
				}
				else if (pb1 == pa2) {
					// 赤黄・黄黄
					firstmove = new Action(PuyoDirection.DOWN, 1);
					secondmove = new Action(PuyoDirection.DOWN, 0);
					if (pc1 == pc2) {
						if (pc1 != pb1) {
							// 赤黄・黄黄・赤赤
							// 赤黄・黄黄・緑緑
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
						else {
							// 赤黄・黄黄・黄黄
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
					}
					else if (pc1 == pb1) {
						if (pc2 == pa1) {
							// 赤黄・黄黄・黄赤
							thirdmove = new Action(PuyoDirection.DOWN, 1);
						}
						else {
							// 赤黄・黄黄・黄緑
							thirdmove = new Action(PuyoDirection.LEFT, 1);
						}
					}
					else if (pc2 == pb1) {
						if (pc1 == pa1) {
							// 赤黄・黄黄・赤黄
							thirdmove = new Action(PuyoDirection.UP, 1);
						}
						else {
							// 赤黄・黄黄・緑黄
							thirdmove = new Action(PuyoDirection.RIGHT, 0);
						}
					}
					else {
						// 赤黄・黄黄・赤緑
						// 赤黄・黄黄・緑赤
						// 赤黄・黄黄・緑青
						thirdmove = null;
					}
				}
				else {
					// 赤黄・緑緑
					if (pc1 == pc2) {
						if (pc1 == pa1) {
							// 赤黄・緑緑・赤赤
						}
						else if (pc1 == pa2) {
							// 赤黄・緑緑・黄黄
						}
						else if (pc1 == pb1) {
							// 赤黄・緑緑・緑緑
						}
						else {
							// 赤黄・緑緑・青青
						}
					}
					else {
					}
				}
			}
			else {
				if (pb1 == pa1) {
					if (pb2 == pa2) {
						// 赤黄・赤黄
					}
					else {
						// 赤黄・赤緑
					}
				}
				else if (pb2 == pa1) {
					if (pb1 == pa2) {
						// 赤黄・黄赤
					}
					else {
						// 赤黄・緑赤
					}
				}
				else if (pb1 == pa2) {
					// 赤黄・黄緑
				}
				else if (pb2 == pa2) {
					// 赤黄・緑黄
				}
				else {
					// 赤黄・緑青
				}
			}
		}
	}
}
