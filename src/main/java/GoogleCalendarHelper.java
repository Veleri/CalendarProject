
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.HttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.DateTime;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.calendar.CalendarScopes;
import com.google.api.services.calendar.model.Event;
import com.google.api.services.calendar.model.EventDateTime;
import com.google.api.services.calendar.model.Events;


import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.*;

public class GoogleCalendarHelper {

    private static final String APPLICATION_NAME = "Google Calendar API SumDU";
    private static final java.io.File DATA_STORE_DIR = new java.io.File(System.getProperty("user.home"), ".credentials/calendar-java");
    private static FileDataStoreFactory DATA_STORE_FACTORY;
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static HttpTransport HTTP_TRANSPORT;
    private static final List<String> SCOPES = Arrays.asList(CalendarScopes.CALENDAR);

    static {
        try {
            HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
            DATA_STORE_FACTORY = new FileDataStoreFactory(DATA_STORE_DIR);
        } catch (Throwable t) {
            System.exit(1);
        }
    }

    private static Credential authorize() throws IOException {
        File file = new File("client_secret.json");
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new FileReader(file));
        System.out.printf(file.getName());
        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow =
                new GoogleAuthorizationCodeFlow.Builder(
                        HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                        .setDataStoreFactory(DATA_STORE_FACTORY)
                        .setAccessType("offline")
                        .build();
        return new AuthorizationCodeInstalledApp(flow, new LocalServerReceiver()).authorize("user");
    }

    private static com.google.api.services.calendar.Calendar getCalendarService() {
        Credential credential = null;
        try {
            credential = authorize();

        } catch (IOException e) {
           throw new RuntimeException(e);
        }
        return new com.google.api.services.calendar.Calendar.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, credential)
                .setApplicationName(APPLICATION_NAME)
                .build();
    }

    public static List<Event> getCalendarSchedule() {
        com.google.api.services.calendar.Calendar service = getCalendarService();
        Calendar c = new GregorianCalendar();
        c.set(Calendar.HOUR_OF_DAY, 0);
        c.set(Calendar.MINUTE, 0);
        c.set(Calendar.SECOND, 0);
        DateTime now = new DateTime(c.getTime().getTime());
        Events events = null;

        try {
            events = service.events().list("primary")
                    .setMaxResults(50)
                    .setTimeMin(now)
                    .setOrderBy("startTime")
                    .setSingleEvents(true)
                    .execute();
        } catch (IOException e) {
        }
        List<Event> items = Objects.requireNonNull(events).getItems();
        //items.removeIf(e -> !e.getSummary().contains("SUMDU"));
        for (Event e : items) {
            e.setSummary(e.getSummary().replace("SumDuScheduler_Veleri", ""));
        }
        return items;
    }

    public static void postEvent(EventItem item) {
        com.google.api.services.calendar.Calendar service = getCalendarService();
        String calendarId = "primary";
        Event event = null;
        try {
            event = classToEvent(item);
            event = service.events().insert(calendarId, event).execute();
        } catch (IOException e) {
        }
        String summary = event == null ? "NONE" : event.getSummary();
        String htmlLink = event == null ? "NONE" : event.getHtmlLink();
        System.out.printf("Event created: %s %s\n", summary, htmlLink);
    }

    public static void deleteEvent(String eventId) {
        com.google.api.services.calendar.Calendar service = getCalendarService();
        try {
            service.events().delete("primary", eventId).execute();
        } catch (IOException e) {
        }
    }

    public static Event classToEvent(EventItem item) {

        SimpleDateFormat formatter = new SimpleDateFormat("dd.MM.yyyyHH:mm");
        Date startDate = null;
        Date endDate = null;
        try {
            startDate = formatter.parse(item.getDate() + item.getTime().substring(0, item.getTime().indexOf("-")));
            endDate = formatter.parse(item.getDate() + item.getTime().substring(item.getTime().indexOf("-") + 1));
        } catch (ParseException e) {
           e.printStackTrace();
        }
        String description =
                "subject: " + item.getDiscipline()
                        + "\nteacher: " + item.getTeacher()
                        + "\ngroup: " + item.getGroups()
                        + "\ntype: " + item.getType()
                        + "\nclassroom: " + item.getClassroom()
                        + "\nday: " + item.getDay();

        Event event = new Event()
                .setSummary("SumDuScheduler_Veleri_" + item.getDiscipline())
                .setDescription(description);
        DateTime startDateTime = new DateTime(startDate);
        EventDateTime start = new EventDateTime()
                .setDateTime(startDateTime)
                .setTimeZone("Europe/Kiev");
        event.setStart(start);
        DateTime endDateTime = new DateTime(endDate);
        EventDateTime end = new EventDateTime()
                .setDateTime(endDateTime)
                .setTimeZone("Europe/Kiev");
        event.setEnd(end);
        return event;
    }
	
}
