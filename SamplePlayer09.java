package player;



import sp.AbstractSamplePlayer;

import sp.ConnectionCounter;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.AbstractPlayer;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Action;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Board;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Field;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.Puyo.PuyoDirection;
import jp.ac.nagoya_u.is.ss.kishii.usui.system.game.PuyoPuyo;

/*
getScoreによって返される得点が高い配置方法を選択する．
getScoreを改良して強いプレイヤーを作ってみよう．
 */


/**
 * ぷよを設置したときの盤面のスコアが最大になる配置を選択するプレイヤー
 * 
 * @author tori
 */
public class SamplePlayer09 extends AbstractSamplePlayer {


	@Override
	public Action doMyTurn() {

		/**
		 * 現在のフィールドの状況
		 */
		Field field = getMyBoard().getField();


		Action action = null;
		int maxScore = 0;
		for(int i = 0; i < field.getWidth(); i++){
			for(PuyoDirection dir:PuyoDirection.values()){

				/**
				 * 配置不可能，または負けてしまうような配置は最初から考慮しない
				 */
				if(!isEnable(dir, i)){
					continue;
				}

				int score = getScore(i, dir);
				if(score > maxScore){
					action = new Action(dir, i);
					maxScore = score;
				}
			}
		}

		if(action == null){
			System.out.println("Default");
			action = getDefaultAction();
		}
		System.out.println("----------------------");
		printField(field);
		System.out.printf("%d-%s(%d)\n", action.getColmNumber(), action.getDirection(), maxScore);
		System.out.println("----------------------");

		return action;
	}

	/**
	 * スコアリング．
	 * もっともぷよが少なくなる場合を最大スコアとする．
	 * また，高さが低いほど良いとする．
	 * @param x
	 * @param dir
	 * @return
	 */
	private int getScore(int x, PuyoDirection dir) {
		Field field = getMyBoard().getField();
		Puyo puyo = getMyBoard().getCurrentPuyo();
		puyo.setDirection(dir);
		Field nextField = field.getNextField(puyo, x);

		int totalPuyoNum = 0;
		for(int i = 0; i < nextField.getWidth(); i++){
			totalPuyoNum += nextField.getTop(i);
		}
		
		int score = nextField.getHeight()*nextField.getWidth()-totalPuyoNum;

		return score;
	}

	/**
	 * 配置可能か，あるいは死んでしなわないかをチェックする
	 * @param i
	 * @param dir
	 * @return 配置不能または死んでしまう場合はfalse
	 */
	private boolean isEnable(PuyoDirection dir, int i) {
		Field field = getMyBoard().getField();

		//配置不能ならfalse
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

	/**
	 * 指定したフィールドのぷよ数を返す
	 * @param field
	 * @return
	 */
	int getPuyoNum(Field field){
		int num = 0;
		//ここでぷよの数を数える．
		//field.getTop(columnNum)で，ぷよが存在する場所を返すので，
		//それより1大きい数のぷよがその列には存在する
		//ぷよが一つもない列は-1が返ってくることに注意．

		for(int i = 0; i < field.getWidth(); i++){
			num+=field.getTop(i)+1;
		}

		return num;
	}

	/**
	 * 特に配置する場所がなかった場合の基本行動
	 * @return
	 */
	Action getDefaultAction(){
		Board board = getGameInfo().getBoard(getMyPlayerInfo());
		Field field = board.getField();
		int minColumn = 0;
		for(int i = 0; i < field.getWidth(); i++){
			if(field.getTop(i) < field.getTop(minColumn)){
				minColumn = i;
			}
		}

		Action action = new Action(PuyoDirection.DOWN, minColumn);


		return action;
	}


	public void printField(Field field){
		for(int y = field.getHeight(); y >= 0 ; y--){
			for(int x = 0; x < field.getWidth(); x++){
				if(field.getPuyoType(x, y) != null){
					System.out.print(field.getPuyoType(x, y).toString().substring(0, 1));
				}
				else{
					System.out.print(".");
				}
			}
			System.out.println();
		}
	}

	public static void main(String args[]) {
		AbstractPlayer player = new SamplePlayer09();

		PuyoPuyo puyopuyo = new PuyoPuyo(player);
		puyopuyo.puyoPuyo();
	}
}
