module de.tuebingen.sfs.psl.gui {
	exports de.tuebingen.sfs.psl.gui;
	requires de.tuebingen.sfs.psl;
	requires psl.core;
    requires h2;
    requires java.desktop;
    requires javafx.base;
    requires javafx.graphics;
    requires javafx.controls;
    requires javafx.fxml;
    opens de.tuebingen.sfs.psl.gui;
//    opens fx;
}