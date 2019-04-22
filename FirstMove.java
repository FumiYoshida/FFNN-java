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
	
	public Function<Board, Action> ActionAfterNexNexSeen;
	
	public void Main(Board myboard) {
		Puyo pa = myboard.getCurrentPuyo();
		Puyo pb = myboard.getNextPuyo();
		Puyo pc = myboard.getNextNextPuyo();
		if (pa.getPuyoType(PuyoNumber.SECOND) == pa.getPuyoType(PuyoNumber.FIRST)) {
			if (pb.getPuyoType(PuyoNumber.FIRST) == pb.getPuyoType(PuyoNumber.SECOND)) {
				if (pa.getPuyoType(PuyoNumber.FIRST) == pb.getPuyoType(PuyoNumber.FIRST)) {
					// 最初の4色すべて同じ色だった場合
					// 隣に置いて全消しをする
					firstmove = new Action(PuyoDirection.DOWN, 2);
					secondmove = new Action(PuyoDirection.DOWN, 3);
				}
				else {
					// 上下同色のぷよが2つ来た場合（赤赤・黄黄など）
					
				}
			}
			else{
			}
		}
		else {
		}
	}
}
