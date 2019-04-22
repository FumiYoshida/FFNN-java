package player;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PuyoPuyo;
import sp.AbstractSamplePlayer;


/*
 * 左から二番目に積み上げるように改良してみよう．
 */

/**
 * 一番左にだけ積み上げるエージェント．
 * 
 * @author tori
 *
 */
public class UrusaiPlayer extends AbstractSamplePlayer {



	@Override
	public Action doMyTurn() {
		/**
		 * ぷよを置く場所
		 */
		int columnNum = 0;

		/**
		 * 縦にぷよを配置する命令
		 */
		
		Puyo curpuyo = getMyBoard().getCurrentPuyo();
		for (int i=0;i<4;i++) {

			System.out.print(curpuyo.getDirection() + " -> ");
			System.out.println("top is " + curpuyo.getTopPuyoNumber());
			System.out.print(curpuyo.getDirection() + " -> ");
			System.out.println("bottom is " + curpuyo.getBottomPuyoNumber());
			System.out.println(curpuyo.getSecondColmNumber());
			curpuyo.rotate();
		}


		Action action = new Action(PuyoDirection.DOWN, columnNum);
		return action;
	}


	/**
	 * おまじない
	 * @param args
	 */
	public static void main(String args[]) {
		AbstractPlayer player1 = new SamplePlayer01();

		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}
