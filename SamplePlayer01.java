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
import java.util.Random;


/*
 * 左から二番目に積み上げるように改良してみよう．
 */

/**
 * 一番左にだけ積み上げるエージェント．
 * 
 * @author tori
 *
 */
public class SamplePlayer01 extends AbstractSamplePlayer {



	@Override
	public Action doMyTurn() {
		/**
		 * ぷよを置く場所
		 */
		int columnNum = 0;
		/**
		 * 縦にぷよを配置する命令
		 */
		Random rd = new Random(System.currentTimeMillis());
		Puyo curpuyo = getMyBoard().getCurrentPuyo();
		int rotatenum = rd.nextInt(4);
		for (int i=0;i<rotatenum;i++) {
			curpuyo.rotate();
		}
		if (rd.nextDouble() < 0.01) {
			columnNum = rd.nextInt(6);
		}
		else {
			int mintop = 12;
			for (int i=0;i<6;i++) {
				if (getMyBoard().getField().getTop(i) < mintop) {
					mintop = getMyBoard().getField().getTop(i);
					columnNum = i;
				}
			}
		}
		Action action = new Action(curpuyo, rd.nextInt(6));
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
