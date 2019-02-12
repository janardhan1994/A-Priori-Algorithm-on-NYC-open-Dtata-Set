/*
 *  CC03 : A-priori algorithm implementation to find strong association between nyc 311 contact center service request details to cut cost.
 *  Project Desc:
 In order to find the solution, we need to find where money was spent. 1. headcount 2. expense.
 To analysis this, we have created augmented column in this format : <casetype><month><weekendorweekday><timetakenresolve ticket in hours bucket>
1. case type : there are 3 types : COMP [citizen complaint about issue example: neighbour loud-speaker, parking in front of walking area] , REQ [citizen requesting service or task to be performed example clean up dead animals],
INFO: looking for information like school closings, recycling rules, homeless shelters, park events, pothole repairs etc.
==> this will help us to cut call volume[for information type calls]. Use chatbot, or update website with latest details. find the way to connect with people to avoid information type case.
2.weekendorweekday : getting service ticket on weekend or after hours is expensive since deploying resource during this timeframe is high compared with normal business hours.
3. afterhourorbusinesshour : after hours call is expensive and this will help work allocation and scheduling with shift using optimization will cut the cost.
4.shift : Morning,evening,night and late night after hour shift for 311 contact center. this trend will tell which shift we can put more resource and where we can cut resource.
5.timetakentoresolve the ticket: this will help us to avoid repeated call[increase call volume means increasing expenses] and low hanging fruit can be automated instead of deploying resource.

Ideally we need to create all this 5 column separately in order to find relationship among these variables. since this project requested to create 1column. we concatenated to find the relationship between this variable and
Complaint Type,Agency,City.

Example : COMPDecWEEKDAYAFTERHRNIGHTOPEN ==>complaint case on dec month weekday after hour night and status of ticket still open.
COMPDecWEEKDAYAFTERHRNIGHT5292 ==> complaint case on dec month weekday after hour night and time taken to close this ticket 5292 hours.
COMPDecWEEKDAYAFTERHRNIGHT1 ==> this one close one hour bucket.
COMPDecWEEKDAYAFTERHRNIGHT0 ==> less than one hours.

Command to run the code:

java -Xmx100m AP -g < ../311.txt > 1col.txt
java -Xmx100m AP -v .03 .4  < ../small.txt
java -Xmx100m AP -m .01 .9 ../311.txt 1col.txt

Note: JDK 1.8 REQUIRED FOR THIS CODE TO RUN, WE USED FEW PACKAGES FROM 1.8

 */

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.NoSuchElementException;
import java.util.Scanner;


class itemSetTree {

	public void addchild(final String[] itemset, final HashMap<String, itemSetTree> node) {

		itemSetTree childNode;

		if (itemset.length == 1) {

			childNode = node.get("root");
			child[0] = childNode;
			childNode.addParentNode(this);
		} else {

			for (int i = 0; i < itemset.length; i++) {

				final String[] subset = new String[itemset.length - 1];

				String subsetString = "";
				String sep = "";

				for (int j = 0; j < itemset.length; j++) {
					if (j < i) {
						subset[j] = itemset[j];
						subsetString = subsetString + sep + subset[j];
						sep = ",";
					} else if (j > i) {
						subset[j - 1] = itemset[j];
						subsetString = subsetString + sep + subset[j - 1];
						sep = ",";
					}
				}

				if (node.containsKey(subsetString) == false) {

					childNode = new itemSetTree(node, subset, leafRecord);
					node.put(subsetString, childNode);
				} else {

					childNode = node.get(subsetString);
					childNode.incrementRecord(leafRecord);
				}

				child[i] = childNode;

				childNode.addParentNode(this);
			}
		}
	}

	@Override
	public String toString() {
		String output = "List items:";
		for (int i = 0; i < itemset.length; i++) {
			output = output + " " + itemset[i];
		}

		output = output + "; count: " + count + ";: " + leafRecord;

		return output;
	}

	public void addParentNode(final itemSetTree newParentNode) {

		final itemSetTree[] parent2 = new itemSetTree[parent.length + 1];

		for (int i = 0; i < parent.length; i++) {
			parent2[i] = parent[i];
		}

		parent2[parent.length] = newParentNode;

		parent = parent2;
	}

	public void incrementRecord(final int record) {
		if (record > leafRecord) {
			count++;
			leafRecord = record;

			if (child.length > 1) {
				for (int i = 0; i < child.length; i++) {
					child[i].incrementRecord(record);
				}
			}
		}
	}

	public boolean pruneItemSets(final double minsup, final double minconf, final int record,
			final HashMap<String, itemSetTree> node) {

		final double support = (double) count / (double) record;

		if (support < minsup && itemset.length != 0) {

			removeItemSet(node);
			return true;
		} else {

		}

		for (int i = 0; i < parent.length; i++) {

			if (parent[i] == null) {
				continue;
			}

			if (parent[i].pruneItemSets(minsup, minconf, record, node)) {
				parent[i].deleteItemSet(node);
			} else {

			}
		}

		return false;

	}

	public String itemsetToString() {
		if (itemset.length < 1) {
			return "root";
		} else {
			String output = itemset[0];
			for (int i = 1; i < itemset.length; i++) {
				output = output + "," + itemset[i];
			}
			return output;
		}
	}

	public void removeItemSet(final HashMap<String, itemSetTree> node) {
		for (int i = 0; i < parent.length; i++) {
			if (parent[i] != null) {
				parent[i].deleteItemSet(node);
			}
		}
	}

	public void printResults(final String ii) {

		for (int i = 0; i < parent.length; i++) {
			parent[i].printResults(ii + "  ");
		}
	}

	public void deleteItemSet(final HashMap<String, itemSetTree> node) {

		node.remove(itemsetToString());

		for (int i = 0; i < child.length; i++) {
			for (int j = 0; j < child[i].parent.length; j++) {
				if (child[i].parent[j] != null && child[i].parent[j].itemsetToString().equals(itemsetToString())) {

					child[i].parent[j] = null;
				}
			}
		}

		for (int i = 0; i < parent.length; i++) {
			if (parent[i] != null) {
				parent[i].deleteItemSet(node);
			}
		}
	}

	public void validation(final String left, final String right, final String union,
			final HashMap<String, Float[]> map, final HashMap<String, itemSetTree> node) {

		if (node.containsKey(left + "->" + right) == false) {

			final String rule = left + "->" + right;
			final float count1 = node.get(union).count;
			final float count2 = node.get(left).count;
			final float count3 = count1 / count2;

			map.put(rule, new Float[] { count3, count1 });
		}
	}

	public void genItemSetRules(final HashMap<String, Float[]> map, final HashMap<String, itemSetTree> node) {

		if (itemset.length < 3) {

			for (int i = 0; i < parent.length; i++) {
				if (parent[i] != null) {
					parent[i].genItemSetRules(map, node);
				}
			}
		} else if (itemset.length == 3) {

			validation(itemset[0], itemset[1], itemset[0] + "," + itemset[1], map, node);
			validation(itemset[1], itemset[0], itemset[0] + "," + itemset[1], map, node);
			validation(itemset[0], itemset[2], itemset[0] + "," + itemset[2], map, node);
			validation(itemset[2], itemset[0], itemset[0] + "," + itemset[2], map, node);
			validation(itemset[1], itemset[2], itemset[1] + "," + itemset[2], map, node);
			validation(itemset[2], itemset[1], itemset[1] + "," + itemset[2], map, node);
			validation(itemset[0], itemset[1] + "," + itemset[2], itemsetToString(), map, node);
			validation(itemset[1] + "," + itemset[2], itemset[0], itemsetToString(), map, node);
			validation(itemset[1], itemset[0] + "," + itemset[2], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[2], itemset[1], itemsetToString(), map, node);
			validation(itemset[2], itemset[0] + "," + itemset[1], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[1], itemset[2], itemsetToString(), map, node);

			for (int i = 0; i < parent.length; i++) {
				if (parent[i] != null) {
					parent[i].genItemSetRules(map, node);
				}
			}
		} else {

			validation(itemset[0], itemset[1] + "," + itemset[2] + "," + itemset[3], itemsetToString(), map, node);
			validation(itemset[1] + "," + itemset[2] + "," + itemset[3], itemset[0], itemsetToString(), map, node);
			validation(itemset[1], itemset[0] + "," + itemset[2] + "," + itemset[3], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[2] + "," + itemset[3], itemset[1], itemsetToString(), map, node);
			validation(itemset[2], itemset[0] + "," + itemset[1] + "," + itemset[3], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[1] + "," + itemset[3], itemset[2], itemsetToString(), map, node);
			validation(itemset[3], itemset[0] + "," + itemset[1] + "," + itemset[2], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[1] + "," + itemset[2], itemset[3], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[1], itemset[2] + "," + itemset[3], itemsetToString(), map, node);
			validation(itemset[2] + "," + itemset[3], itemset[0] + "," + itemset[1], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[2], itemset[1] + "," + itemset[3], itemsetToString(), map, node);
			validation(itemset[1] + "," + itemset[3], itemset[0] + "," + itemset[2], itemsetToString(), map, node);
			validation(itemset[1] + "," + itemset[2], itemset[0] + "," + itemset[3], itemsetToString(), map, node);
			validation(itemset[0] + "," + itemset[3], itemset[1] + "," + itemset[2], itemsetToString(), map, node);
		}
	}

	public itemSetTree(final HashMap<String, itemSetTree> node, final String[] itemset, final int recordcount) {

		this.itemset = itemset.clone();
		count = 1;
		leafRecord = recordcount;

		parent = new itemSetTree[0];

		if (itemset.length == 0) {

			child = null;
		} else {
			child = new itemSetTree[itemset.length];
			addchild(itemset, node);
		}

	}

	public int getCount() {
		return count;
	}

	public void setCount(int count) {
		this.count = count;
	}

	public int getLeafRecord() {
		return leafRecord;
	}

	public void setLeafRecord(int leafRecord) {
		this.leafRecord = leafRecord;
	}

	public String[] getItemset() {
		return itemset;
	}

	public void setItemset(String[] itemset) {
		this.itemset = itemset;
	}

	public itemSetTree[] getChild() {
		return child;
	}

	public void setChild(itemSetTree[] child) {
		this.child = child;
	}

	public itemSetTree[] getParent() {
		return parent;
	}

	public void setParent(itemSetTree[] parent) {
		this.parent = parent;
	}

	int count;
	int leafRecord;
	String[] itemset;
	itemSetTree[] child;
	itemSetTree[] parent;

}

/**
 * The Class AP.
 */
public class AP {

	/** The option. */
	static String option = "";

	/**
	 * Gets the option.
	 *
	 * @return the option
	 */
	public static String getOption() {
		return option;
	}

	/**
	 * Sets the option.
	 *
	 * @param option the new option
	 */
	public static void setOption(String option) {
		AP.option = option;
	}

	/**
	 * Gets the minsup.
	 *
	 * @return the minsup
	 */
	public static String getMinsup() {
		return minsup;
	}

	/**
	 * Sets the minsup.
	 *
	 * @param minsup the new minsup
	 */
	public static void setMinsup(String minsup) {
		AP.minsup = minsup;
	}

	/**
	 * Gets the minconf.
	 *
	 * @return the minconf
	 */
	public static String getMinconf() {
		return minconf;
	}

	/**
	 * Sets the minconf.
	 *
	 * @param minconf the new minconf
	 */
	public static void setMinconf(String minconf) {
		AP.minconf = minconf;
	}

	/**
	 * Gets the dataset.
	 *
	 * @return the dataset
	 */
	public static String getDataset() {
		return dataset;
	}

	/**
	 * Sets the dataset.
	 *
	 * @param dataset the new dataset
	 */
	public static void setDataset(String dataset) {
		AP.dataset = dataset;
	}

	/**
	 * Gets the augmenteddataset.
	 *
	 * @return the augmenteddataset
	 */
	public static String getAugmenteddataset() {
		return augmenteddataset;
	}

	/**
	 * Sets the augmenteddataset.
	 *
	 * @param augmenteddataset the new augmenteddataset
	 */
	public static void setAugmenteddataset(String augmenteddataset) {
		AP.augmenteddataset = augmenteddataset;
	}

	/** The minsup. */
	static String minsup = "";

	/** The minconf. */
	static String minconf = "";

	/** The dataset. */
	static String dataset = "";

	/** The augmenteddataset. */
	static String augmenteddataset = "";

	/**
	 * The main method.
	 *
	 * @param args the arguments
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void main(final String[] args) throws IOException {

		if (args.length < 1) {
			System.out.println(
					"ERROR: No arguments provided. Please pass correct arguments : java AP -g < DATASET > AugmentedDataset or java AP -v minsup minconf < DATASET or java AP -m minsup minconf DATASET AugmentedDataset");

			System.exit(0);
		}

		option = args[0];

		switch (option) {
		case "-g":

			if (args.length == 1) {
				// dataset = args[3];
				generateAugmentedDataSet();

			} else {
				System.out.println("ERROR: Incorrect argument . Please pass correct arguments : java AP -g < DATASET");

			}
			break;

		case "-v":
			if (args.length == 3) {
				final float minsup = Float.parseFloat(args[1]);
				final float minconf = Float.parseFloat(args[2]);
				// dataset = args[4];

				if (checkNumber(minsup) && checkNumber(minconf)) {
					generateFrequencyItemSetWithVerbose(minsup, minconf);
				} else {
					System.out.println("ERROR: value of minsup/minconf is not between 0 to 1" + "minsup-" + minsup
							+ "minconf-" + minconf);

				}

			} else {
				System.out.println(
						"ERROR : Incorrect argument . Please pass correct arguments :java AP -v minsup minconf < DATASET");
			}
			break;

		case "-m":
			if (args.length == 5) {

				final float minsup = Float.parseFloat(args[1]);
				final float minconf = Float.parseFloat(args[2]);
				dataset = args[3];
				augmenteddataset = args[4];

				if (checkFile(dataset) && checkFile(augmenteddataset) && checkNumber(minsup) && checkNumber(minconf)) {
					generateStrongRulesWithAugmentedDataset(minsup, minconf, dataset, augmenteddataset);
				} else {
					System.out.println("ERROR: dataset is not found or value of minsup/minconf is not between 0 to 1"
							+ "dataset-" + dataset + "augmenteddataset-" + augmenteddataset + "minsup-" + minsup
							+ "minconf-" + minconf);

				}

			} else {
				System.out.println(
						"ERROR : Incorrect argument . Please pass correct arguments : java AP -m minsup minconf DATASET AugmentedDataset");
			}
			break;

		default:
			System.out.println("ERROR : Incorrect argument . Please pass correct option argument.");
			System.exit(0);
		}

	}

	/**
	 * Generate strong rules with augmented dataset.
	 *
	 * @param minsup the minsup
	 * @param minconf the minconf
	 * @param dataset the dataset
	 * @param augmenteddataset the augmenteddataset
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void generateStrongRulesWithAugmentedDataset(final float minsup, final float minconf,
			final String dataset, final String augmenteddataset) throws IOException {

		final HashMap<String, Float[]> map = new HashMap<>();

		Date starttime = new Date();

		String datarecord = "";
		String augmentedrecord = "";

		final Scanner inputfileReader = new Scanner(new File(dataset));
		final Scanner inputaugmentedfileReader = new Scanner(new File(augmenteddataset));

		final HashMap<String, itemSetTree> node = new HashMap<>();

		final itemSetTree root = new itemSetTree(node, new String[0], 0);
		node.put("root", root);

		String[] feature1 = null;
		String[] feature2 = null;

		int record = 1;
		while (inputfileReader.hasNext()) {

			datarecord = inputfileReader.nextLine();
			augmentedrecord = inputaugmentedfileReader.nextLine();

			feature1 = parseServiceRequest(datarecord, 46);
			feature2 = parseServiceRequest(augmentedrecord, 2);

			final String[] features = new String[] { feature1[3], feature1[5], feature1[19], feature2[1] };

			final String indexString = feature1[3] + "," + feature1[5] + "," + feature1[19] + "," + feature2[1];

			if (node.containsKey(indexString)) {

				node.get(indexString).incrementRecord(record);
			} else {

				final itemSetTree itemset1 = new itemSetTree(node, features, record);
				node.put(indexString, itemset1);
			}

			record++;

		}

		root.pruneItemSets(minsup, minconf, record, node);
		root.genItemSetRules(map, node);

		String r = "";
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			r = it.next();
			if (map.get(r)[0] < minconf) {
				try {
					it.remove();
				} catch (final UnsupportedOperationException e) {

				} catch (final IllegalStateException e) {

				}
			}
		}

		it = map.keySet().iterator();

		int i = 0;
		while (it.hasNext()) {
			r = it.next();
			i++;
			System.out.println(r + " (sup = " + String.format("%.3f", map.get(r)[1] / record) + " conf = "
					+ String.format("%.3f", map.get(r)[0]) + ")");
		}

		if (i == 0) {
			System.out
			.println("INFO: No node found after pruning . please lower thresold value for minsup and minconf");
		}

		inputfileReader.close();
		inputaugmentedfileReader.close();
		Date endtime = new Date();
		double interval = endtime.getTime() - starttime.getTime();
		System.out.println("Total time taken: " + interval / 1000.0 + " seconds." + "nodesize:" + node.size());

	}

	/**
	 * Generate frequency item set with verbose.
	 *
	 * @param minsup the minsup
	 * @param minconf the minconf
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void generateFrequencyItemSetWithVerbose(final float minsup, final float minconf) throws IOException {

		String line = "";

		Scanner inputfileReader = new Scanner(System.in);

		final HashMap<String, itemSetTree> node = new HashMap<>();

		final itemSetTree root = new itemSetTree(node, new String[0], 0);
		node.put("root", root);

		String[] feature;

		int record = 1;

		while (inputfileReader.hasNext()) {

			line = inputfileReader.nextLine();
			feature = parseServiceRequest(line, 46);

			final String[] features = new String[] { feature[3], feature[5], feature[19] };

			final String indexString = feature[3] + "," + feature[5] + "," + feature[19];

			if (node.containsKey(indexString)) {

				node.get(indexString).incrementRecord(record);
			} else {

				final itemSetTree itemset1 = new itemSetTree(node, features, record);
				node.put(indexString, itemset1);
			}

			record++;

		}

		root.pruneItemSets(minsup, minconf, record, node);

		final HashMap<String, Float[]> map = new HashMap<>();

		root.genItemSetRules(map, node);

		String rule = "";
		Iterator<String> it = map.keySet().iterator();
		while (it.hasNext()) {
			rule = it.next();
			if (map.get(rule)[0] < minconf) {
				System.out.println(rule + " c = " + String.format("%.3f", map.get(rule)[0]) + " pruned");

				try {
					it.remove();
				} catch (final UnsupportedOperationException e) {

				} catch (final IllegalStateException e) {

				}
			} else {

				System.out.println(rule + " c = " + String.format("%.3f", map.get(rule)[0]) + " not pruned");

			}
		}

		if (map.size() == 0) {
			System.out
			.println("INFO: No node found after pruning . please lower thresold value for minsup and minconf");
		}
		it = map.keySet().iterator();
		while (it.hasNext()) {
			rule = it.next();

		}

		inputfileReader.close();

	}

	/**
	 * Generate augmented data set.
	 *
	 * @throws IOException Signals that an I/O exception has occurred.
	 */
	public static void generateAugmentedDataSet() throws IOException {
		String vector = "";
		String augmentedfeature_singlecolum = "";
		String augmentedfeature_shift = "";
		String augmentedfeature_casetype = "";
		String augmentedfeature_month = "";
		String augmentedfeature_timetaken = "";
		Scanner inputdatasetReader = new Scanner(System.in);

		String[] augmentedfeatureArray = null;

		while (inputdatasetReader.hasNext()) {
			vector = inputdatasetReader.nextLine();
			augmentedfeatureArray = parseServiceRequest(vector, 46);
			/*
			 * System.out.println("list of colums"+augmentedfeatureArray);
			 *
			 * for (int i = 0; i < augmentedfeatureArray.length; i++) { String value =
			 * augmentedfeatureArray[i]; System.out.println("Element: " + value); }
			 */

			if (augmentedfeatureArray[5].equals("Complaint Type") && augmentedfeatureArray[6].equals("Descriptor")
					&& augmentedfeatureArray[16].equals("Resolution Description")
					&& augmentedfeatureArray[1].equals("Created Date") && augmentedfeatureArray[2].equals("Closed Date")
					&& augmentedfeatureArray[14].equals("Status")) {
				augmentedfeature_singlecolum = "augmentedcolum";
			} else {
				augmentedfeature_singlecolum = getSingleColum(augmentedfeatureArray[5], augmentedfeatureArray[6],
						augmentedfeatureArray[16], augmentedfeatureArray[1], augmentedfeatureArray[2],
						augmentedfeatureArray[14]);
			}

			//
			// if ( augmentedfeatureArray[5].equals("Complaint Type") &&
			// augmentedfeatureArray[6].equals("Descriptor") &&
			// augmentedfeatureArray[16].equals("Resolution Description")) {
			// augmentedfeature_casetype = "casetype";
			// } else {
			// augmentedfeature_casetype =
			// getCaseType(augmentedfeatureArray[5],augmentedfeatureArray[6],augmentedfeatureArray[16]);
			// }
			//
			// if (augmentedfeatureArray[1].equals("Created Date")) {
			// augmentedfeature_month = "month";
			// } else {
			// augmentedfeature_month = getMonth(augmentedfeatureArray[1]);
			// }
			//
			//
			// if (augmentedfeatureArray[1].equals("Created Date")) {
			// augmentedfeature_shift = "shift";
			// } else {
			// augmentedfeature_shift = getShift(augmentedfeatureArray[1]);
			// }
			//
			//
			// if (augmentedfeatureArray[1].equals("Created Date") &&
			// augmentedfeatureArray[2].equals("Closed Date") &&
			// augmentedfeatureArray[14].equals("Status")) {
			// augmentedfeature_timetaken = "timetaken";
			// } else {
			// augmentedfeature_timetaken =
			// getResolutionTime(augmentedfeatureArray[1],augmentedfeatureArray[2],augmentedfeatureArray[14]);
			// }

			// System.out.println(augmentedfeatureArray[0] + "," +
			// augmentedfeature_casetype+ "," + augmentedfeature_month+ "," +
			// augmentedfeature_shift+ "," + augmentedfeature_timetaken);

			System.out.println(augmentedfeatureArray[0] + "," + augmentedfeature_singlecolum);

		}
		inputdatasetReader.close();

	}

	/**
	 * Parses the service request.
	 *
	 * @param vector the vector
	 * @param length the length
	 * @return the string[]
	 */
	public static String[] parseServiceRequest(final String vector, final int length) {

		final String[] feature = new String[length];

		Scanner linereader = new Scanner(vector);
		final Scanner featurescanner = linereader.useDelimiter(",");

		for (int i = 0; i < length; i++) {
			try {

				if (i == length - 1) {
					if (featurescanner.findWithinHorizon("\"", 2) != null) {
						featurescanner.useDelimiter("\"");
						feature[i] = featurescanner.next();
					} else {
						feature[i] = featurescanner.next();
					}

				} else if (featurescanner.findWithinHorizon("\"", 2) != null) {
					featurescanner.useDelimiter("\",");
					feature[i] = featurescanner.next();
					featurescanner.findWithinHorizon("\"", 1);
					featurescanner.useDelimiter(",");

				} else {
					feature[i] = featurescanner.next();
				}
			} catch (final NoSuchElementException e) {
				feature[i] = "ERROR NoSuchElementException : feature is not found";
			}
		}

		featurescanner.close();
		linereader.close();
		return feature;
	}

	/**
	 * Gets the day.
	 *
	 * @param date the date
	 * @return the day
	 */
	public static String getDay(final String date) {

		@SuppressWarnings("deprecation")
		final Date now = new Date(date);

		final SimpleDateFormat simpleDateformat = new SimpleDateFormat("E");
		return simpleDateformat.format(now);

	}

	/**
	 * Time to seconds.
	 *
	 * @param time the time
	 * @return the long
	 */
	static long timeToSeconds(final String time) {
		long ret = 0;
		final String[] ar = time.split("\\:");
		for (int i = 0; i < ar.length; i++) {
			ret += Long.valueOf(ar[i]) * Math.pow(60, (2 - i));
		}
		return ret;
	}

	/**
	 * Checks if is time between.
	 *
	 * @param startTime the start time
	 * @param endTime the end time
	 * @param currentTime the current time
	 * @return true, if is time between
	 */
	static boolean isTimeBetween(final String startTime, final String endTime, final String currentTime) {
		final long lCurrentTime = timeToSeconds(currentTime);
		final long lstartTime = timeToSeconds(startTime);
		final long lEndTime = timeToSeconds(endTime);

		if (((lstartTime - lCurrentTime) * (lEndTime - lCurrentTime) * (lstartTime - lEndTime)) > 0) {
			return true;
		} else {
			return false;
		}
	}

	/**
	 * Gets the shift.
	 *
	 * @param date the date
	 * @return the shift
	 */
	public static String getShift(final String date) {

		String shift = "AFTERHRLATENIGHT";
		@SuppressWarnings("deprecation")
		final Date now = new Date(date);

		final SimpleDateFormat simpleDateformat1 = new SimpleDateFormat("H:m:s");
		final String hoursofday = simpleDateformat1.format(now);

		if (isTimeBetween("05:59:59", "08:59:59", hoursofday)) {

			shift = "AFTERHRMORNING";
		}

		if (isTimeBetween("16:59:59", "17:59:59", hoursofday)) {

			shift = "AFTERHREVENING";
		}

		if (isTimeBetween("17:59:59", "23:59:59", hoursofday)) {

			shift = "AFTERHRNIGHT";
		}

		if (isTimeBetween("00:00:00", "05:59:59", hoursofday)) {

			shift = "AFTERHRLATENIGHT";
		}

		if (isTimeBetween("08:59:59", "16:59:59", hoursofday)) {

			shift = "BUSINESSHR";
		}
		return shift;

	}

	/**
	 * Gets the weekend.
	 *
	 * @param date the date
	 * @return the weekend
	 */
	public static String getWeekend(final String date) {

		String weekend = "WEEKDAY";
		if (getDay(date).contains("Sat") || getDay(date).contains("Sun")) {
			weekend = "WEEKEND";
		}
		return weekend;

	}

	/**
	 * Gets the single colum.
	 *
	 * @param ComplaintType the complaint type
	 * @param Description the description
	 * @param ResolutionDescription the resolution description
	 * @param createdDate the created date
	 * @param closedDate the closed date
	 * @param Status the status
	 * @return the single colum
	 */
	public static String getSingleColum(final String ComplaintType, final String Description,
			final String ResolutionDescription, final String createdDate, final String closedDate,
			final String Status) {

		String singleColum = "";

		singleColum = getCaseType(ComplaintType, Description, ResolutionDescription) + getMonth(createdDate)
		+ getWeekend(createdDate) + getShift(createdDate) + getResolutionTime(createdDate, closedDate, Status);

		return singleColum;

	}

	/**
	 * Contains item from array.
	 *
	 * @param inputString the input string
	 * @param items the items
	 * @return true, if successful
	 */
	public static boolean containsItemFromArray(String inputString, String[] items) {
		// Convert the array of String items as a Stream
		// For each element of the Stream call inputString.contains(element)
		// If you have any match returns true, false otherwise
		return Arrays.stream(items).anyMatch(inputString::contains);
	}

	/**
	 * Gets the case type.
	 *
	 * @param ComplaintType the complaint type
	 * @param Description the description
	 * @param ResolutionDescription the resolution description
	 * @return the case type
	 */
	public static String getCaseType(final String ComplaintType, final String Description,
			final String ResolutionDescription) {

		String casetype = "NA";

		String[] searchForInfo = { "school closings", "recycling rules", "homeless shelters", "park events",
				"pothole repairs", "Alternate Side Parking status information", "holiday schedules",
				"school and government office closure information", "weather", "closures due to parades and events",
				"Information", "Communication", "Notification", "Looking", "INFORMATION", "COMMUNICATION",
				"NOTIFICATION", "LOOKING", "INFO", "Weather", "closings", "holiday", "status", "Staus", "inquiry"

		};

		String[] searchForRequest = { "request", "service", "Request", "Service" };

		if (containsItemFromArray(ComplaintType, searchForInfo) || containsItemFromArray(Description, searchForInfo)
				|| containsItemFromArray(Description, searchForInfo)) {
			casetype = "INFO";

		} else if (containsItemFromArray(ComplaintType, searchForRequest)
				|| containsItemFromArray(Description, searchForRequest)
				|| containsItemFromArray(Description, searchForRequest)) {
			casetype = "REQ";
		} else {
			casetype = "COMP";
		}

		return casetype;

	}

	/**
	 * Gets the month.
	 *
	 * @param date the date
	 * @return the month
	 */
	public static String getMonth(final String date) {

		@SuppressWarnings("deprecation")
		final Date now = new Date(date);

		final SimpleDateFormat simpleDateformat = new SimpleDateFormat("MMM");

		return simpleDateformat.format(now);

	}

	/**
	 * Gets the resolution time.
	 *
	 * @param createdDate the created date
	 * @param closedDate the closed date
	 * @param Status the status
	 * @return the resolution time
	 */
	public static String getResolutionTime(final String createdDate, final String closedDate, final String Status) {

		String resolutionTime = "OPEN";

		if ("Closed".equalsIgnoreCase(Status) && !createdDate.equals("") && !closedDate.equals("")) {
			DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MM/dd/yyyy hh:mm:ss a");

			LocalDateTime formatcreatedDate = LocalDateTime.parse(createdDate, formatter);
			LocalDateTime formatclosedDate = LocalDateTime.parse(closedDate, formatter);

			long duration = Duration.between(formatcreatedDate, formatclosedDate).toHours();
			resolutionTime = Long.toString(duration);
		}
		return resolutionTime;

	}

	/**
	 * Check file.
	 *
	 * @param fileName the file name
	 * @return true, if successful
	 */
	public static boolean checkFile(final String fileName) {
		boolean filestatus = false;
		final File f = new File(fileName);
		if (f.exists() && f.isFile()) {
			filestatus = true;
		}
		return filestatus;

	}

	/**
	 * Check number.
	 *
	 * @param number the number
	 * @return true, if successful
	 */
	public static boolean checkNumber(final float number) {
		return 0.0 <= number && number <= 1.0;
	}

}
