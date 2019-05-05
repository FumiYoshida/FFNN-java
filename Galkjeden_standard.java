package player;

import java.util.ArrayList;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.GameInfo.PlayerNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoNumber;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.storage.PuyoType;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PuyoPuyo;
import sp.AbstractSamplePlayer;


public class Galkjeden_standard extends AbstractSamplePlayer {
	
	
	@Override
	public Action doMyTurn() {
		Board board = getMyBoard();
		Field field = board.getField();
		Puyo curpuyo = board.getCurrentPuyo();
		Puyo nexpuyo = board.getNextPuyo();
		Puyo nexnexpuyo = board.getNextNextPuyo();
		List<Integer> ojal = board.getNumbersOfOjamaList();
		int width = field.getWidth();
		int height = field.getHeight();
		int myactionnum = 0;
		int grace = 1;
		int scorethreshold = 500; // 重要
		int scorethreshold2 = 500; 
		for (int oja : ojal) {
			if (oja > 0) {
				break;
			}
			grace++;
		}		
		Action myaction = null;
		RecursiveNextField rnf = new RecursiveNextField();
		rnf.MakeAllActions(getMyBoard());
		myactionnum = rnf.availablemypuyocolumns.length;
		int[] maxsumscores = new int[myactionnum];
		int[] firstscores = new int[myactionnum];
		if (grace < 3) {
			/* 2ターン以内（最長でもこのターンの次のターンが終わったとき）
			 *  におじゃまが降ってくるとき、おじゃまが降る前に連鎖を発火させる。
			 *  発火できないときはカウンター形を築く。
			 */
			if (grace == 1) {
				for (int i=0;i<myactionnum;i++) {
					RecursiveNextField rnf1 = new RecursiveNextField();
					rnf1.Calc(rnf.availablemypuyos[i], nexpuyo, rnf.availablemypuyocolumns[i], field);
					maxsumscores[i] = rnf1.myscoreincrement;
				}
			}
			else if (grace == 2) {
				for (int i=0;i<myactionnum;i++) {
					RecursiveNextField rnf1 = new RecursiveNextField();
					rnf1.Calc(rnf.availablemypuyos[i], nexpuyo, rnf.availablemypuyocolumns[i], field);
					firstscores[i] = rnf1.myscoreincrement;
					for (int j=0;j<rnf1.availablemypuyocolumns.length;j++) {
						RecursiveNextField rnf2 = new RecursiveNextField();
						rnf2.Calc(rnf1.availablemypuyos[j], nexnexpuyo, rnf1.availablemypuyocolumns[j], rnf1.nextfield);
						int temp = rnf1.myscoreincrement + rnf2.myscoreincrement;
						if (temp > maxsumscores[i]) {
							maxsumscores[i] = temp;
						}
					}
				}
			}
			int maxscore = 0;
			int selectindex = 0;
			for (int i=0;i<myactionnum;i++) {
				if (maxsumscores[i] > maxscore) {
					maxscore = maxsumscores[i];
					selectindex = i;
				}
			}
			if (grace == 2 && maxscore < scorethreshold2) {
				// 点数を得られない場合はsampleplayer08と同じ挙動を取る
				// すぐには発火させないものをなるたけ選んでいる
				myaction = Sample08(rnf.availablemypuyos, rnf.availablemypuyocolumns, firstscores);
			}
			if (myactionnum == 0) {
				myaction = new Action(PuyoDirection.DOWN, 0);
			}
			else if (myaction == null) {
				myaction = new Action(rnf.availablemypuyos[selectindex], rnf.availablemypuyocolumns[selectindex]);
			}
		}
		else {
			/* まだしばらくは猶予があるとき
			 *  3ターン以内（ネクネクまで使ったとき）に、もしくは
			 *  望ましい色が1つでも来れば4ターン以内発火させることが可能な手
			 * （つまりは発火点をつぶさない手）の内、その発火する連鎖数が最も大きい手を選ぶ。
			 */
			for (int i=0;i<myactionnum;i++) {
				RecursiveNextField rnf1 = new RecursiveNextField();
				rnf1.Calc(rnf.availablemypuyos[i], nexpuyo, rnf.availablemypuyocolumns[i], field);
				firstscores[i] = rnf1.myscoreincrement;
				for (int j=0;j<rnf1.availablemypuyocolumns.length;j++) {
					RecursiveNextField rnf2 = new RecursiveNextField();
					rnf2.Calc(rnf1.availablemypuyos[j], nexnexpuyo, rnf1.availablemypuyocolumns[j], rnf1.nextfield);
					for (int k=0;k<rnf2.availablemypuyocolumns.length;k++) {
						RecursiveNextField rnf3 = new RecursiveNextField();
						rnf3.Calc(rnf2.availablemypuyos[k], null, rnf2.availablemypuyocolumns[k], rnf2.nextfield);
						int temp = rnf1.myscoreincrement + rnf2.myscoreincrement + rnf3.myscoreincrement;
						if (temp > maxsumscores[i]) {
							maxsumscores[i] = temp;
						}
					}
					
				}
			}
			int maxscore = -10000;
			int selectindex = -10000;
			double temp = 0;
			for (int i=0;i<myactionnum;i++) {
				if (maxsumscores[i] - firstscores[i] * temp > maxscore) {
					maxscore = (int) (maxsumscores[i] - firstscores[i] * temp);
					selectindex = i;
				}
			}
			if (maxscore < scorethreshold) {
				// 点数を得られない場合はsampleplayer08と同じ挙動を取る
				// すぐには発火させないものをなるたけ選んでいる
				myaction = Sample08(rnf.availablemypuyos, rnf.availablemypuyocolumns, firstscores);
			}
			if (myactionnum == 0) {
				myaction = new Action(PuyoDirection.DOWN, 0);
			}
			else if (myaction == null) {
				myaction = new Action(rnf.availablemypuyos[selectindex], rnf.availablemypuyocolumns[selectindex]);
			}
		}
		return myaction;
	}
	
	
	public Action Sample08(Puyo[] availablepuyos, int[] availablecolumns, int[] scores) {
		Action action = null;
		Field field = getMyBoard().getField();
		Puyo puyo = getMyBoard().getCurrentPuyo();
		int maxNeighborPuyo = 0;
		for(int i = 0; i < field.getWidth(); i++){
			for(PuyoDirection dir:PuyoDirection.values()){
				if(!isEnable(dir, i) || ScoreOfAction(dir, i, availablepuyos, availablecolumns, scores) != 0){
					continue;
				}
				PuyoType firstPuyo = puyo.getPuyoType(PuyoNumber.FIRST);
				PuyoType secondPuyo = puyo.getPuyoType(PuyoNumber.SECOND);
				int firstNeighbor = 0;
				int secondNeighbor = 0;

				//最初のぷよの周りに存在する同色ぷよ数を数える
				if(dir == PuyoDirection.DOWN){
					//二番目のぷよが下にある場合は，topの二つ上がy座標
					int y = field.getTop(i)+2;
					firstNeighbor = getNeighborPuyoNum(i, y, firstPuyo);
				}
				else{
					//二番目のぷよが下にある場合以外は，topの1つ上がy座標
					int y = field.getTop(i)+1;
					firstNeighbor = getNeighborPuyoNum(i, y, firstPuyo);
				}

				//二番目のぷよの周りに存在する同色ぷよを数える
				if(dir == PuyoDirection.DOWN){
					//二番目のぷよが下にある場合
					int y = field.getTop(i)+1; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				else if(dir == PuyoDirection.UP){
					//二番目のぷよが上にある場合
					int y = field.getTop(i)+2; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				else if(dir == PuyoDirection.RIGHT){
					//二番目のぷよが右にある場合
					int y = field.getTop(i)+1; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				else if(dir == PuyoDirection.LEFT){
					//二番目のぷよが左にある場合
					int y = field.getTop(i)+1; 
					secondNeighbor = getNeighborPuyoNum(i, y, secondPuyo);
				}
				if (firstNeighbor + secondNeighbor > maxNeighborPuyo) {
					maxNeighborPuyo = firstNeighbor + secondNeighbor;
					action = new Action(dir, i);
				}
			}
		}
		return action;
	}
	
	private int ScoreOfAction(PuyoDirection dir, int column, Puyo[] availablepuyos, int[] availablecolumns, int[] scores) {
		for (int i=0;i<availablecolumns.length;i++) {
			if (dir == availablepuyos[i].getDirection() && column == availablecolumns[i]) {
				return scores[i];
			}
		}
		return -1;
	}
	
	private boolean isEnable(PuyoDirection dir, int i) {
		Field field = getMyBoard().getField();
		if(!field.isEnable(dir, i)){
			return false;
		}
		if(dir == PuyoDirection.DOWN || dir == PuyoDirection.UP){
			if(field.getTop(i) >= field.getDeadLine()-2){
				return false;
			}
		}
		else if(dir == PuyoDirection.RIGHT){
			if(field.getTop(i) >= field.getDeadLine()-2 || field.getTop(i+1) >= field.getDeadLine()-2) {
				return false;
			}
		}
		else if(dir == PuyoDirection.LEFT){
			if(field.getTop(i) >= field.getDeadLine()-2 || field.getTop(i-1) >= field.getDeadLine()-2) {
				return false;
			}
		}
		return true;
	}

	private int getNeighborPuyoNum(int x, int y, PuyoType puyoType) {
		//数を記録する変数
		int count = 0;
		Field field = getMyBoard().getField();	
		if (x - 1 >= 0) {
			if (field.getPuyoType(x-1, y) == puyoType) {
				count++;
			}
		}
		if (x + 1 < field.getWidth()) {
			if (field.getPuyoType(x+1, y) == puyoType) {
				count++;
			}
		}
		if (y - 1 >= 0) {
			if (field.getPuyoType(x, y-1) == puyoType) {
				count++;
			}
		}
		if (y + 1 < field.getHeight()) {
			if (field.getPuyoType(x, y+1) == puyoType) {
				count++;
			}
		}
		return count;
	}
	
	/**
	 * おまじない
	 * @param args
	 */
	public static void main(String args[]) {
		AbstractPlayer player1 = new Galkjeden_standard();
		PuyoPuyo puyopuyo = new PuyoPuyo(player1);
		puyopuyo.puyoPuyo();
	}
}
