package player;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PlayerInfo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;

public class QLearning {
	Puyo[] availablemypuyos;
	Puyo[] availableenemypuyos;
	int[] availablemypuyocolumns;
	int[] availableenemypuyocolumns;
	
	public void Settings(Board board, PlayerInfo me, Board enemyboard, PlayerInfo enemy) {
		MakeAllActions(board, enemyboard);
		int myactionnum = availablemypuyos.length;
		int enemyactionnum = availableenemypuyos.length;
		NextField[] mynexts = new NextField[myactionnum];
		NextField[] enemynexts = new NextField[enemyactionnum];
		int[] sprungmyojama = new int[myactionnum];
		int[] sprungenemyojama = new int[enemyactionnum];
		for (int i=0;i<enemyactionnum;i++) {
			enemynexts[i] = new NextField();
			enemynexts[i].Settings(availableenemypuyos[i], availableenemypuyocolumns[i], enemyboard, enemy, board);
			sprungenemyojama[i] = enemynexts[i].myscore;
		}
		for (int i=0;i<myactionnum;i++) {
			mynexts[i] = new NextField();
			mynexts[i].Settings(availablemypuyos[i], availablemypuyocolumns[i], board, me, enemyboard);
		}
	}
	
	public void MakeAllActions(Board board, Board enemyboard) {
		// 自分の行動最大22通り、相手の行動最大22通り、ネクネクの次のぷよ15通り（同じ：5通り、異なる：10通り）で分岐
		// それぞれ独立なので共通部分はコピーする
		// まず自分と相手がどんな行動をとれるかを考える
		Field myfield = board.getField();
		Field enemyfield = enemyboard.getField();
		boolean[] myactions = new boolean[11];
		boolean[] enemyactions = new boolean[11];
		for (int i=0;i<6;i++) {
			// 縦に設置できるか
			myactions[i] = myfield.getTop(i) < 10;
			enemyactions[i] = enemyfield.getTop(i) < 10;
		}
		for (int i=0;i<5;i++) {
			// 横に設置できるか
			myactions[i+6] = myfield.getTop(i) < 11 && myfield.getTop(i+1) < 11;
			enemyactions[i+6] = enemyfield.getTop(i) < 11 && enemyfield.getTop(i+1) < 11;
		}
		int myactionnum = 0;
		int enemyactionnum = 0;
		for (int i=0;i<11;i++) {
			if (myactions[i]) {
				myactionnum++;
			}
			if (enemyactions[i]) {
				enemyactionnum++;
			}
		}
		Puyo curpuyo = board.getCurrentPuyo();
		PuyoType firstpuyo = curpuyo.getPuyoType(PuyoNumber.FIRST);
		PuyoType secondpuyo = curpuyo.getPuyoType(PuyoNumber.SECOND);
		Puyo[] puyos = MakePuyos(curpuyo);
		Puyo[] mypuyos;
		Puyo[] enemypuyos;
		int[] mysetcols;
		int[] enemysetcols;
		int myactionserial = 0;
		int enemyactionserial = 0;
		if (firstpuyo == secondpuyo) {
			mypuyos = new Puyo[myactionnum];
			enemypuyos = new Puyo[enemyactionnum];
			mysetcols = new int[myactionnum];
			enemysetcols = new int[enemyactionnum];
			for (int i=0;i<6;i++) {
				if (myactions[i]) {
					mypuyos[myactionserial] = puyos[0];
					mysetcols[myactionserial] = i;
					myactionserial++;
				}
				if (enemyactions[i]) {
					enemypuyos[enemyactionserial] = puyos[0];
					enemysetcols[enemyactionserial] = i;
					enemyactionserial++;
				}
			}
			for (int i=0;i<5;i++) {
				if (myactions[i+6]) {
					mypuyos[myactionserial] = puyos[3];
					mysetcols[enemyactionserial] = i;
					myactionserial++;
				}
				if (enemyactions[i+6]) {
					enemypuyos[enemyactionserial] = puyos[3];
					enemysetcols[enemyactionserial] = i;
					enemyactionserial++;
				}
			}
		}
		else {
			// 設置するぷよの向きが2通り
			mypuyos = new Puyo[myactionnum*2];
			enemypuyos = new Puyo[enemyactionnum*2];
			mysetcols = new int[myactionnum*2];
			enemysetcols = new int[enemyactionnum*2];
			for (int i=0;i<6;i++) {
				if (myactions[i]) {
					mypuyos[myactionserial] = puyos[0];
					mypuyos[myactionserial+1] = puyos[2];
					mysetcols[myactionserial] = i;
					mysetcols[myactionserial+1] = i;
					myactionserial += 2;
				}
				if (enemyactions[i]) {
					enemypuyos[enemyactionserial] = puyos[0];
					enemypuyos[enemyactionserial+1] = puyos[2];
					enemysetcols[enemyactionserial] = i;
					enemysetcols[enemyactionserial+1] = i;
					enemyactionserial += 2;
				}
			}
			for (int i=0;i<5;i++) {
				if (myactions[i+6]) {
					mypuyos[myactionserial] = puyos[1];
					mypuyos[myactionserial+1] = puyos[3];
					mysetcols[enemyactionserial] = i+1;
					mysetcols[enemyactionserial+1] = i;
					myactionserial += 2;
				}
				if (enemyactions[i+6]) {
					enemypuyos[enemyactionserial] = puyos[1];
					enemypuyos[enemyactionserial+1] = puyos[3];
					mysetcols[enemyactionserial] = i+1;
					mysetcols[enemyactionserial+1] = i;
					enemyactionserial += 2;
				}
			}
		}
		availablemypuyos = mypuyos;
		availableenemypuyos = enemypuyos;
		availablemypuyocolumns = mysetcols;
		availableenemypuyocolumns = enemysetcols;
	}
	
	public Puyo[] MakePuyos(Puyo moto) {
		// 各方向のぷよを作成する
		// 設置できるcolumnnumはUP, DOWNで0~5, RIGHTで0~4, LEFTで1~5
		Map<PuyoNumber, PuyoType> puyotypesmap = moto.getPuyoTypesMap();
		Puyo uepuyo = new Puyo(puyotypesmap);
		uepuyo.setDirection(PuyoDirection.UP);
		Puyo migipuyo = new Puyo(puyotypesmap);
		migipuyo.setDirection(PuyoDirection.RIGHT);
		Puyo shitapuyo = new Puyo(puyotypesmap);
		uepuyo.setDirection(PuyoDirection.DOWN);
		Puyo hidaripuyo = new Puyo(puyotypesmap);
		migipuyo.setDirection(PuyoDirection.LEFT);
		Puyo[] output = new Puyo[] {uepuyo, migipuyo, shitapuyo, hidaripuyo};
		return output;
	}

}
