package com.mg.controller;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.apache.log4j.Logger;
import org.hibernate.Transaction;

import com.mg.csms.beans.Demand;
import com.mg.csms.beans.InwardStockItem;
import com.mg.utils.DBQueriesUtils;
import com.mg.utils.DateUtils;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.DatePicker;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.control.TextField;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.text.Text;

public class DemandController {
	private static Logger log = Logger.getLogger(DemandController.class);

	@FXML
	private DatePicker demandDate;
	@FXML
	private TextField coldNo;
	@FXML
	private TextField quantity;
	@FXML
	private TableView<Demand> itemListTable;
	@FXML
	private TableColumn<Demand, String> tableColdNo;
	@FXML
	private TableColumn<Demand, String> tableQuantity;
	@FXML
	private Text successMessage;
	List<Demand> addItemList;
	private DBQueriesUtils dbQueriesUtils;

	@FXML
	private void initialize() {
		try {
			dbQueriesUtils = new DBQueriesUtils();
		} catch (Exception e) {
			successMessage.setText("Database errors occoured");
		}

		addItemList = new ArrayList<>();
		DateUtils.initializeDate(demandDate);
		itemListTable.setEditable(true);
		tableColdNo.setCellValueFactory(new PropertyValueFactory<Demand, String>("coldNo"));
		tableQuantity.setCellValueFactory(new PropertyValueFactory<Demand, String>("quantity"));
	}

	@FXML
	private void addDemandToList() {
		itemListTable.setItems(getDemandItem());
	}

	private ObservableList<Demand> getDemandItem() {
		addItemList.add(addDemandItem());
		clearUI();
		return FXCollections.observableList(addItemList);
	}

	private Demand addDemandItem() {
		Demand demand = new Demand();
		demand.setQuantity(Integer.parseInt(quantity.getText()));
		demand.setColdNo(Integer.parseInt(coldNo.getText()));
		return demand;
	}

	private void clearUI() {
		coldNo.clear();
		quantity.clear();
	}

	@FXML
	private void submitDemand() {
		try {
			successMessage.setText("");
			Transaction tx = dbQueriesUtils.getSession().beginTransaction();
			makeDemandItemsAndSave();
			tx.commit();
		} catch (Exception e) {
			log.debug(e);
			successMessage.setText("Some Error Occured !!! Make sure you have entererd all fields correctly !");
		}
		if ("".equalsIgnoreCase(successMessage.getText()))
			successMessage.setText("Demand added successfully.");
		clearOverallUI();
	}

	private void makeDemandItemsAndSave() {
		itemListTable.getItems().stream().forEach(demandItem -> {
			Demand demand = new Demand();
			if (isValidColdNo(demandItem.getColdNo())) {
				demand.setColdNo(demandItem.getColdNo());
				demand.setDemandDate(DateUtils.makeDate(demandDate));
				demand.setQuantity(demandItem.getQuantity());
				if (updateStockItemListBalance(demandItem))
					dbQueriesUtils.getSession().save(demand);
			} else
				successMessage.setText("Cold Id " + demandItem.getColdNo()
						+ " is not correct ! Verify that you made an entry in stock earlier.");
		});
	}

	private boolean updateStockItemListBalance(Demand demandItem) {
		Optional<InwardStockItem> item = dbQueriesUtils.getStockItemList().stream()
				.filter(ele -> ele.getColdNo().toString().equalsIgnoreCase(demandItem.getColdNo().toString()))
				.findFirst();
		if (item.isPresent()) {
			if (item.get().getBalance() == 0) {
				if ("".equalsIgnoreCase(successMessage.getText()))
					successMessage.setText(" LOT " + item.get().getColdNo() + "Completed. Balance: 0");
				else
					successMessage.setText(successMessage.getText() + " AND " + " LOT " + item.get().getColdNo()
							+ "Completed. Balance: 0");
				return false;
			}
			Integer balance = item.get().getBalance() - demandItem.getQuantity();
			if (balance < 0) {
				if ("".equalsIgnoreCase(successMessage.getText()))
					successMessage.setText("Insufficient quantity avaiable for COLD No: " + item.get().getColdNo()
							+ ". Available: " + item.get().getBalance());
				else
					successMessage
							.setText(successMessage.getText() + " AND " + "Insufficient quantity avaiable for COLD No: "
									+ item.get().getColdNo() + ". Available: " + item.get().getBalance());
				return false;
			}
			item.get().setBalance(balance);
			dbQueriesUtils.getSession().update(item.get());
			return true;
		} else {
			if ("".equalsIgnoreCase(successMessage.getText()))
				successMessage.setText("Cold No Does not exist. Please enter correct Cold No to place Demand.");
			else
				successMessage.setText(successMessage.getText() + " AND "
						+ "Cold No Does not exist. Please enter correct Cold No to place Demand.");
		}
		return false;
	}

	private boolean isValidColdNo(Integer coldNo) {
		return dbQueriesUtils.getStockItemList().stream().anyMatch(item -> item.getColdNo().equals(coldNo));
	}

	private void clearOverallUI() {
		itemListTable.setItems(null);
		addItemList.clear();
		clearUI();
	}
}
