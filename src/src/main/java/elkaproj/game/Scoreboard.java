package elkaproj.game;

import java.awt.*;
import java.awt.event.ActionListener;
import javax.swing.*;
import javax.swing.event.*;
import java.util.ArrayList;
import java.util.Vector;
import java.util.Collections;
import java.util.Comparator;

/**
 * Implements scoreboard
 */

public class Scoreboard extends JPanel{

    private class Score{
        private String name;
        private int points;

        public Score(String name, int points){
            this.name=name;
            this.points=points;
        }

        public String getName() {
            return name;
        }

        public int getPoints() {
                return points;
        }

    }

    /**
     * Lista wyników
     */

    ArrayList<Score> ScoreList;

    /**
     * Konstruktor
     * @param panelWidth width of panel
     * @param panelHeight heigth of panel
     * @param menuListener controller listener
     */

    public Scoreboard(int panelWidth, int panelHeight,
                          ActionListener menuListener) {

        setLayout(new BorderLayout());
        setPreferredSize(new Dimension(panelWidth, panelHeight));
        ScoreList = new ArrayList<>();
        add(makeScoreboardTable(), BorderLayout.CENTER);
        add(makeScoreboardLabel(), BorderLayout.NORTH);
        add(makeBackToMenuButton(menuListener), BorderLayout.SOUTH);
        setVisible(true);
        }

        /**
         * generator tabeli wyników
         * @return tabela wyników
         */

    private JTable makeScoreboardTable() {
        sortList();
        Vector<Vector> rowData = new Vector<>();

        for (int i = 0; i < 10; i++) {
            Vector<String> row = new Vector<>();
            String rowNumber = Integer.toString(i + 1);
            row.add(rowNumber);
            row.add(ScoreList.get(i).getName());
            row.add(Integer.toString(ScoreList.get(i).getPoints()));
            rowData.add(row);
        }

        Vector<String> colLabels = new Vector<>();
        colLabels.addElement("-");
        colLabels.addElement("Nick");
        colLabels.addElement("Wynik");

            JTable highScoreLabel = new JTable(rowData, colLabels);
            highScoreLabel.setEnabled(false);
            highScoreLabel.getTableHeader().setReorderingAllowed(false);

            return highScoreLabel;
    }

    /**
     * Generator przycisku powrotu do menu
     * @param menuListener listener menu głownego
     * @return przycisk powrotu do menu
     */
    private JButton makeBackToMenuButton(ActionListener menuListener) {
        JButton backToMainMenuBtn = new JButton("Back to main menu");
        backToMainMenuBtn.setFocusable(false);
        backToMainMenuBtn.addActionListener(menuListener);
        backToMainMenuBtn.setActionCommand("BackToMenuFromScorePanel");
        return backToMainMenuBtn;
    }

    /**
     * Generator etykiety panelu wyników
     * @return etykieta penelu
     */
    private JLabel makeScoreboardLabel() {
        JLabel highScoreLbl = new JLabel("<html><br> Scoreboard <br><br></html>");
        highScoreLbl.setHorizontalAlignment(JLabel.CENTER);
        highScoreLbl.setVerticalAlignment(JLabel.CENTER);
        highScoreLbl.setFont(new Font("Arial", Font.PLAIN, 15));
        return highScoreLbl;
    }
    /**
     * Sortowanie listy wyników
     */

     private void sortList() {
        Collections.sort(ScoreList, new Comparator<Score>() {
                public int compare(Score score, Score temp) {
                    if(score.getPoints()<temp.getPoints()){
                        return 1;
                    }
                    if(score.getPoints()>temp.getPoints())
                    {
                        return -1;
                    }
                    else
                        return 0;
                }
            });
        }

        /**
         * Dodanie wyniku do
         * @param Name nick gracza
         * @param Points liczba punktow
         */

        private void addScore(String Name, int Points){
            ScoreList.add(new Score(Name, Points));
            sortList();
        }
}