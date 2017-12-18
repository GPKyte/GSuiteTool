import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.googleapis.json.GoogleJsonResponseException;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.client.util.ArrayMap;

import com.google.api.services.admin.directory.Directory;
import com.google.api.services.admin.directory.DirectoryScopes;

import com.google.api.services.admin.directory.model.User;
import com.google.api.services.admin.directory.model.Users;
import com.google.api.services.admin.directory.model.UserName;
import com.google.api.services.admin.directory.model.UserOrganization;
import com.google.api.services.admin.directory.model.Member;
import com.google.api.services.admin.directory.model.Groups;
import com.google.api.services.admin.directory.model.Group;

import java.io.IOException;
import java.io.FileNotFoundException;
import java.io.BufferedReader;
import java.io.FileReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.File;
import java.util.Arrays;
import java.util.List;
import java.util.ArrayList;
import java.util.HashMap;

/**
 * Tool to interact with Users and Groups in a GSuite domain
 * Requires an API key, Admin access, and an internet connection.
 * The main method makes use of flag arguments,
 * use --help to see available options.
 *
 * @author Gavin Kyte
 * @version 3.1.1 (10.18.2017)
 */
public class GSuiteTool {
    /** Application name. */
    private static final String APPLICATION_NAME =
        "Directory API GSuite Tool";

    /** Directory to store user credentials for this application. */
    private static final java.io.File DATA_STORE_DIR = new java.io.File(
        System.getProperty("user.home"), ".credentials/admin-directory_v1-gsuite-tool");

    /** Global instance of the {@link FileDataStoreFactory}. */
    private static FileDataStoreFactory DATA_STORE_FACTORY;

    /** Global instance of the JSON factory. */
    private static final JsonFactory JSON_FACTORY =
        JacksonFactory.getDefaultInstance();

    /** Global instance of the HTTP transport. */
    private static HttpTransport HTTP_TRANSPORT;

    /** Global instance of Directory service. */
    private static Directory service;

    /** Global instance of scopes required
     *
     * If modifying these scopes, delete your previously saved credentials
     * at ~/.credentials/admin-directory_v1-gsuite-tool
     */
    private static final List<String> SCOPES =
        Arrays.asList(
            DirectoryScopes.ADMIN_DIRECTORY_GROUP_MEMBER,
            DirectoryScopes.ADMIN_DIRECTORY_USER,
            DirectoryScopes.ADMIN_DIRECTORY_GROUP_READONLY
        );

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            t.printStackTrace();
            System.exit(1);
        }
    }

    /**
     * Creates an authorized Credential object.
     * @return an authorized Credential object.
     * @throws IOException If "client_secret.json" not included
     *                     per Google Admin SDK instructions
     */
    public static Credential authorize() throws IOException {
        // Load client secrets.
        InputStream in =
            GSuiteTool.class.getResourceAsStream("/client_secret.json");
        GoogleClientSecrets clientSecrets =
            GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
            new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(DATA_STORE_FACTORY)
                .setAccessType("offline")
                .build();
        Credential credential = new AuthorizationCodeInstalledApp(
            flow, new LocalServerReceiver()).authorize("user");
        System.out.println(
            "Credentials saved to " + DATA_STORE_DIR.getAbsolutePath());
        return credential;
    }

    /**
     * Build and return an authorized Admin SDK Directory client service.
     * @return an authorized Directory client service
     * @throws IOException  If authorize() fails to find
     *                      necessary files for authorization
     */
    public static Directory getDirectoryService() throws IOException {
        Credential credential = authorize();
        return new Directory.Builder(
            HTTP_TRANSPORT, JSON_FACTORY, credential)
            .setApplicationName(APPLICATION_NAME)
            .build();
    }

    /////////////////////////////////
    //////   PERSONAL FIELDS  ///////
    /////////////////////////////////

    /** Other globals for flag usage
     * Want to refactor this design; suggestions are welcome
     */
    private static boolean dryRun = false;
    private static boolean verbose = true;
    private static boolean listUsers = false;
    private static boolean listGroups = false;
    private static boolean addMembers = false;
    private static boolean createUsers = false;
    private static boolean example = false;
    private static boolean update = false;
    private static boolean testing = false;
    private static boolean debug = false;
    private static String path = "";

    /////////////////////////////////
    /////////////////////////////////
    ///////   COMMAND FLAGS   ///////
    /////////////////////////////////
    /////////////////////////////////

    /**
     * Enum class with intent of holding flags for our
     * applications main method in GSuiteTools
     */
    enum Flag {
        ADD("-a --add --groups",
                "Add members to a group. [REQUIRES PATH]"),
        CREATE("-c --create",
                "Create new users. [REQUIRES PATH]"),
        DEBUG("-d --debug",
                "Print extra details for debugging."),
        DRY("-n --dry",
                "Do a dryRun. Doesn't make any changes."),
        EXAMPLE("-e --example",
                "Print out a line of the expected headers for imports."),
        HELP("-? -h --help",
                "Print this message then exit."),
        LISTGROUPS("-lg --list-groups",
                "List all groups in domain."),
        LISTUSERS("-lu --list-users",
                "List all users in domain."),
        PATH("-p --path",
                "Defines next arg as (full) path to UTF-8 csv formated data file."),
        RESET("-r --reset",
                "Reset permission levels after changing scopes."),
        TEST("-t --test",
                "Test method for new implementations."),
        UPDATE("-u --update",
                "Update organization fields of users. [REQUIRES PATH]"),
        VERBOSE("-v --verbose",
                "Turn on full output.");

        String aliases;
        String description;

        private Flag(String als, String desc) {
            this.aliases = als;
            this.description = desc;
        }

        // Want a way to simplify this
        public void go(String[] args, int index) {
            switch (this) {
                case ADD        :   addMembers = true;
                                    break;
                case CREATE     :   createUsers = true;
                                    break;
                case DEBUG      :   debug = true;
                                    break;
                case DRY        :   dryRun = true;
                                    break;
                case EXAMPLE    :   example = true;
                                    break;
                case HELP       :   help();
                                    break;
                case LISTGROUPS :   listGroups = true;
                                    break;
                case LISTUSERS  :   listUsers = true;
                                    break;
                case PATH       :   path = args[index+1];
                                    break;
                case RESET      :   deletePermissions();
                                    break;
                case TEST       :   testing = true;
                                    break;
                case UPDATE     :   update = true;
                                    break;
                case VERBOSE    :   verbose = true;
                                    break;
                default         :   System.out.printf("The flag %s is not implemented, but it matched %s%n", args[index], this);
                                    break;
            }
        }

        public boolean matches(String flag) {
            for (String al : aliases.split(" ")) {
                if (al.equals(flag)) {
                    return true;
                }
            }
            return false;
        }

        public String getAliases() {
            return aliases;
        }
        public String getDescription() {
            return description;
        }
    }

    /////////////////////////////////
    /////////////////////////////////
    ///////  GSUITE  METHODS  ///////
    /////////////////////////////////
    /////////////////////////////////

    /**
     * List each user in the Domain
     * @throws IOException If API command cannot be completed as called
     */
    private static void listUsers() throws IOException {
        if (dryRun) {
            String name = new Object(){}.getClass().getEnclosingMethod().getName();
            System.out.println("Dry-run not implemented for "+name+" - Aborting method");
            return;
        }
        try {
            String token = "";
            System.out.println("Users:");
            System.out.println("firstName,lastName,email,title");
            while (token != null) {
                Users result = service.users().list()
                    .setMaxResults(500)
                    .setPageToken(token)
                    .setCustomer("my_customer")
                    .setOrderBy("givenName")
                    .setProjection("full")
                    .execute();

                List<User> users = result.getUsers();
                if (users == null || users.size() == 0) {
                    System.out.println("No users found.");
                    return;
                }
                for (User user : users) {
                    String title = "";
                    try {
                        // Suppressing here since it is almost guaranteed that
                        // ArrayList<ArrayMap<String, Object>> is used for user organizations.
                        // If not, this exception is caught by ClassCastException
                        @SuppressWarnings("unchecked")
                        ArrayMap<String, String> organizations = (ArrayMap)((ArrayList)user.getOrganizations()).get(0);
                        title = organizations.get("title");
                        if (title == null) {title="";}
                    } catch (NullPointerException e) { // organizations is probably null
                        title = "";
                    } catch (ClassCastException ce ) { // wrong data structure casted above
                        title = "";
                        System.out.println(user.getPrimaryEmail()+" does not have standard structure for title/dept fields.");
                    }
                    System.out.println(user.getName().getGivenName()+","
                    +user.getName().getFamilyName()+","
                    +user.getPrimaryEmail()+","
                    +title);
                }
                token = result.getNextPageToken();
            }
        } catch (GoogleJsonResponseException e) {
            System.out.println(" - Could not list users");
            if (verbose) {System.out.println(" - Error: "+e);}
        }
    }

    /**
     * List every group in Domain
     * @throws IOException If API command cannot be completed as called
     */
    private static void listGroups() throws IOException {
        if (dryRun) {
            String name = new Object(){}.getClass().getEnclosingMethod().getName();
            System.out.println("Dry-run not implemented for "+name+" - Aborting method");
            return;
        }
        try {
            String token = "";
            while (token != null) {
                Groups result = service.groups().list()
                    .setCustomer("my_customer")
                    .setPageToken(token)
                    .execute();
                List<Group> groups = result.getGroups();
                if (groups == null || groups.size() == 0) {
                    System.out.println("No groups found.");
                } else {
                    System.out.println("Groups:");
                    for (Group g : groups) {
                        System.out.println(g.getName()+", "+g.getEmail());
                    }
                    token = result.getNextPageToken();
                }
            }
        } catch (GoogleJsonResponseException e) {
            System.out.println(" - Could not list groups");
            if (verbose) {System.out.println(" - Error: "+e);}
        }
    }

    /**
     * Add existing users as members to list of groups
     * @param ArrayList<UserData> List of users to add as members to given groups.
     * @throws IOException If API call fails. Usually if service is not instantiated.
     */
    private static void addMembers(ArrayList<UserData> roster) throws IOException {
        if (dryRun) {
            String name = new Object(){}.getClass().getEnclosingMethod().getName();
            System.out.println("Dry-run not implemented for "+name+" - Aborting method");
            return;
        }
        for (UserData data : roster) {
            String email = data.getEmail();
            String[] groups = data.getGroups().split(" ");
            if (groups.length == 0) {
                System.out.println("No groups provided for "+email);
                continue;
            }

            Member m = new Member();
            m.setEmail(email);

            for (int i=0; i<groups.length; i++) {
                System.out.print("INSERT "+email+" INTO "+groups[i]);
                if (dryRun) {
                    System.out.println(" - Dry run enabled");
                } else {
                    try {
                        service.members()
                               .insert(groups[i], m)
                               .execute();
                        System.out.println(" - Success!");
                    } catch (GoogleJsonResponseException e) {
                        System.out.println(" - Failure");
                        if (verbose) {
                            System.out.println(" - "+email+" was not added to group "+groups[i]);
                            System.out.println(" - Error: "+e);
                        }
                    }
                }
            }
        }
    }

    /**
     * Uses csv data to create new users in domain
     * @param ArrayList<UserData> Roster of users to create
     * @throws IOException If API call fails. Usually if service is not instantiated.
     */
    private static void createUsers(ArrayList<UserData> roster) throws IOException {
        if (dryRun) {
            String name = new Object(){}.getClass().getEnclosingMethod().getName();
            System.out.println("Dry-run not implemented for "+name+" - Aborting method");
            return;
        }
        for (UserData data : roster) {
            User u = new User();
            UserName name = new UserName();
                    name.setGivenName(data.getFirstName());
                    name.setFamilyName(data.getLastName());

            u.setName(name);
            u.setPrimaryEmail(data.getEmail());
            u.setPassword(data.getPassword());

            Object newOrgs = data.getOrganizations();
            u.setOrganizations(newOrgs);
            u.setChangePasswordAtNextLogin(true);
            // Not setting phone data but it would be similar to
            // setting organizations in practice

            System.out.print("CREATE "+u.getPrimaryEmail());
            try {
                service.users()
                       .insert(u)
                       .execute();
                System.out.println(" - Success!");
            } catch (GoogleJsonResponseException e) {
                System.out.println(" - Failure");
                System.out.println(" - "+u.getPrimaryEmail()+" could not be created");
                System.out.println(" - Error: "+e);
                System.out.println(u.toPrettyString());
            } catch (NullPointerException n) {
                System.out.println(" - Failure");
                System.out.println(n);
                n.printStackTrace();
            }
        }
    }

    /**
     * Update user title and department info
     * @param ArrayList<UserData> Filled with user data to use in update.
     * @throws IOException when API call to execute fails.
     */
    private static void updateUsers(ArrayList<UserData> roster) throws IOException {
        for (UserData update : roster) {
            String email = update.getEmail();
            // Simple check to see if (Likely) an email address provided
            // A more thorough RegEx is probably unnecessary.
            if (! email.contains("@")) {
              System.out.println(email);
              continue;
            }
            User main = service.users()
                               .get(update.getEmail())
                               .execute();

            if (update.getTitle() == null && update.getDept() == null) {
                main.setOrganizations(update.getOrganizations());
                continue;
            } else {
                main.setOrganizations( updateOrganizations(
                    main.getOrganizations(),
                    update.getOrganizations())
                );
            }

            System.out.printf("UPDATE USER %s", main.getPrimaryEmail());
            try {
                service.users()
                    .update(main.getPrimaryEmail(),
                            main)
                    .execute();
                System.out.println(" - Success!");
            } catch (GoogleJsonResponseException e) {
                System.out.println(" - Failure");
                System.out.println(" - Error: "+e);
                System.out.println(main.toPrettyString());
            }
        }
    }

    /**
     * Method to reset permissions on the case that more scopes are needed
     * or the user running the application changes.
     */
    private static void deletePermissions() {
        try {
            File credentials = new File(DATA_STORE_DIR+"/StoredCredential");
            System.out.println("Credentials deleted: "+credentials.delete());
        } catch (Throwable e) {
            System.out.print("Error trying to delete credentials");
            System.out.print(e);
            e.printStackTrace();
        }
    }

    /** Print help message */
    private static void help() {
        System.out.println("Usage: bash gsuite [OPTIONS] [-p <PATH_TO_DATA_FILE>]");
        System.out.println("");
        System.out.println("    [OPTIONS]");

        for (Flag f : Flag.values()) {
            System.out.printf("     %-32s %s%n", f.getAliases(), f.getDescription());
        }
        System.out.println("     For more help, see documentation in HELPME.md");
        System.out.println("");
    }
    /**
     * Print out a csv formatted header row to use for imports
     */
    private static void exampleData() {
        String[] columns = UserData.getColumns();
        for (int i = 0; i+1 < columns.length; i++) {
            System.out.print(columns[i] + ",");
        }
        System.out.println(columns[columns.length - 1]);
    }

    /**
     * Test method for ideas
     */
    private static void test(ArrayList<UserData> roster) {
        System.out.println("Nothing is being tested currently");
    }

    /////////////////////////////////
    /////////////////////////////////
    ///////  HELPER  METHODS  ///////
    /////////////////////////////////
    /////////////////////////////////

    /**
     * Get the column index of a csv header
     * @param String header row of csv
     * @param String search term
     * @return int column index
     */
    private static int getIndex(String header, String search) {
        int index = -1;
        // CSV's can come into multiple formats, invisible chars need stripped
        String[] columns = header.trim().replaceAll("\\P{Print}", "").split(",");
        for (int i=0; i<columns.length; i++) {
            if (search.equals(columns[i])) {
                index = i;
                break;
            }
        }
        if (index < 0) {System.out.println(search+" not found in file.");}
        return index;
    }

    /**
     * Method to create a checked data structure containing user-data
     * for the API calls to GSuite. Relies on the UserData structure.
     * @param String absolute path to csv file.
     * @return ArrayList<UserData> Collection of users
     */
    private static ArrayList<UserData> parseData(String csvPath) {
        ArrayList<UserData> data = new ArrayList<>();
        try {
            // Open up input stream for csv file
            BufferedReader dataSheet = new java.io.BufferedReader(
                          new java.io.FileReader(csvPath));

            // Find index for each column header
            String header = dataSheet.readLine();
            String[] columns = UserData.getColumns();
            int[] colIndex = new int[columns.length];
            // Create in-order, psuedo dictionary by storing index of each column
            for (int i = 0; i < columns.length; i++) {
                colIndex[i] = getIndex(header, columns[i]);
            }
            // Read each row and set user data
            for (String line = dataSheet.readLine(); line != null; line = dataSheet.readLine()) {
                // Note that the '-1' includes empty Strings to maintain allignment
                String[] row = line.split(",", -1);
                UserData user = new UserData();
                for (int i = 0; i < columns.length; i++) {
                    user.set(columns[i], row[colIndex[i]]);
                }
                data.add(user);
            }
        } catch (FileNotFoundException nf) {
            System.out.println("No file matching \""+csvPath+"\" exists.");
        } catch (IOException io) {
            System.out.println("Exception occured while initially parsing data: "+io);
            System.exit(1);
        }
        return data;
    }

    /**
     * Patches the users title and department fields.
     * @param Object Organizations of GSuite user.
     *        Form of ArrayList<ArrayMap<String, Object>>
     * @param ArrayList Organization fields of update as same object type
     * @return Object Updated Organizations (user's detail fields).
     */
     @SuppressWarnings("unchecked")
    private static Object updateOrganizations(Object main, ArrayList updateOrgs) throws IOException {
        if (updateOrgs == null) {return main;}
        try {
            // Need to cast object here. API-Dependent, SHOULD always be ArrayList<ArrayMap<String, Object>>
            ArrayMap mainFields = (ArrayMap)((ArrayList)main).get(0);
            ArrayMap updateFields = (ArrayMap)updateOrgs.get(0);
            for (Object key : updateFields.keySet()) {
                mainFields.put(key, updateFields.get(key.toString()));
            }
        } catch (ClassCastException e) {
            System.out.println("User's organization fields are not of the expected type");
            System.out.println("Will not update field. Suggest investigating for abnormalities");
            System.out.println(" - Error: "+e);
        }
        return main;
    }

    /////////////////////////////////
    /////////////////////////////////
    ///////    MAIN METHOD    ///////
    /////////////////////////////////
    /////////////////////////////////

    /**
     * Main method handles option flags and calls appropriate functions
     * @param args Optional flags and pathname information
     * @throws IOException If data file cannot be read from or written to
     */
    public static void main(String[] args) throws IOException {
        // Reset global flags
        dryRun = false;
        verbose = false;
        listUsers = false;
        listGroups = false;
        addMembers = false;
        createUsers = false;
        example = false;
        update = false;
        testing = false;
        debug = false;
        path = "";

        // Parse args
        // Could we simplify this with hashmap?
        boolean validCommand;
        for (int i=0; i<args.length; i++) {
            validCommand = false;
            for (Flag command : Flag.values()) {
                if (command.matches(args[i])) {
                    command.go(args, i);
                    validCommand = true;
                    break;
                }
            }

            // Flag not found in any of the commands
            // Check if option is a PATH parameter
            if (!validCommand && !args[i].startsWith("/")) {
                System.out.printf("Command not recognized: '%s'%n", args[i]);
                help();
                System.exit(2);
            }
        }

        // Get help if using tool incorrectly
        if (args.length == 0) {
            System.out.println("No args given");
            help();
            return;
        }

        if (example) {
            System.out.println("Example header row for imports.");
            exampleData();
        }

        // Create data list if requested
        ArrayList<UserData> roster = new ArrayList<>();
        if (addMembers || createUsers || update) {
            if (path.equals("")) {
                System.out.println("--path not specified!");
                help();
                System.exit(3);
            } else {
                roster = parseData(path);
            }
        }

        if ( ! (testing || createUsers || addMembers || update || listGroups || listUsers)) {
            return;
        }
        // Build a new authorized API client service.
        try {
            service = getDirectoryService();
        } catch (NullPointerException n) {
            System.out.println("Missing client_secret.json, cannot init API service");
            n.printStackTrace();
            System.exit(3);
        }
        System.out.println("----------------------------------------------------");

        if (testing) {
            System.out.println("Running test method");
            System.out.println("----------------------------------------------------");
            test(roster);
            System.out.println("----------------------------------------------------");
        }
        if (createUsers) {
            System.out.println("Creating new users from file at "+path);
            System.out.println("----------------------------------------------------");
            createUsers(roster);
            System.out.println("Remember to add these new users to their email distributions next");
            System.out.println("----------------------------------------------------");
        }
        if (addMembers) {
            System.out.println("Adding new members to email distros from file at "+path);
            System.out.println("----------------------------------------------------");
            addMembers(roster);
            System.out.println("----------------------------------------------------");
        }
        if (update) {
            System.out.println("Updating users' titles from file at "+path);
            System.out.println("----------------------------------------------------");
            updateUsers(roster);
            System.out.println("----------------------------------------------------");
        }
        if (listGroups) {
            System.out.println("Listing current groups under our domain");
            System.out.println("----------------------------------------------------");
            listGroups();
            System.out.println("----------------------------------------------------");
        }
        if (listUsers) {
            System.out.println("Listing current users under our domain");
            System.out.println("----------------------------------------------------");
            listUsers();
            System.out.println("----------------------------------------------------");
        }
    }
}

/**
 * Simple object for accessing information on each user
 * Using this structure for consistent naming and iteration
 */
class UserData {
    private ArrayMap<String, String> data;
    // Column headers
    // When column headers change, only need to update these names
    private static final String fName   = "firstName";
    private static final String lName   = "lastName";
    private static final String email   = "email"; // Company email address
    private static final String pw      = "password";
    private static final String title   = "jobtitle";
    private static final String phone   = "phone";
    private static final String dept    = "dept";
    private static final String groups  = "groups";
    // Should contain everything declared above
    private static final String[] columns  = {fName,lName,email,pw,title,phone,dept,groups};
    public UserData() {
      data = new ArrayMap<>();
      // Set default value to null. May not be necessary.
      for (String col : columns) {
        data.put(col, null);
      }
    }
    // Since data not altered elsewhere it's unnecessary to write specific set methods.
    public void set(String key, String value) {data.put(key, value);}
    // BUT specific methods abstracts the datastructure away for multiple get() uses.
    // This reduces errors and time spent looking for the key name.
    public String getFirstName() {return data.get(fName);}
    public String getLastName() {return data.get(lName);}
    public String getEmail() {return data.get(email);}
    public String getPassword() {return data.get(pw);}
    public String getTitle() {return data.get(title);}
    public String getPhone() {return data.get(phone);}
    public String getDept() {return data.get(dept);}
    public String getGroups() {return data.get(groups);}
    public static String[] getColumns() {return columns;}
    // Less necessary but worthy method to simplify update method.
    public ArrayList<ArrayMap<String, Object>> getOrganizations() {
        if (getTitle() == null && getDept() == null) {
            // Not all users need additional fields, leave blank if this is the case.
            return null;
        }
        // Note that this ArrayMap ignores null values
        ArrayList<ArrayMap<String, Object>> fieldsWrapper = new ArrayList<>();
        ArrayMap<String, Object> fields = new ArrayMap<>();
        fields.put("title", getTitle());
        fields.put("dept", getDept());
        fields.put("primary", new Boolean(true));
        fields.put("customType", "");
        fieldsWrapper.add(fields);
        return fieldsWrapper;
    }
}
