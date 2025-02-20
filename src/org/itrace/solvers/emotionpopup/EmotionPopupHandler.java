package org.itrace.solvers.emotionpopup;

import com.google.gson.stream.JsonWriter;
import org.itrace.gaze.IGazeResponse;
import java.awt.Dimension;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import javax.swing.BoxLayout;
import javax.swing.JLabel;
import javax.swing.JOptionPane;
import javax.swing.JPanel;
import javax.swing.JTextField;
import javax.swing.UIManager;

import org.eclipse.core.resources.ResourcesPlugin;
import org.eclipse.e4.core.services.events.IEventBroker;
import org.eclipse.ui.PlatformUI;
import org.osgi.service.event.Event;
import org.osgi.service.event.EventHandler;

public class EmotionPopupHandler implements IEmotionPopupHandler, EventHandler {
    private final long SECONDS_BETWEEN_POPUP = 300;
    private long lastPopup = 0;
    private JsonWriter responseWriter;
    private File outFile;
    private String filename = "emotion-responses-USERNAME-yyMMddTHHmmss-SSSS-Z.json";
    private String sessionID;
    private IGazeResponse lastGazeResponse;

    public EmotionPopupHandler() {
    	UIManager.put("swing.boldMetal", new Boolean(false)); //make UI font plain
    }

    @Override
    public void init() {
        try {
            outFile = new File(getFilename());

            // Check that file does not already exist. If it does, do not begin
            // tracking.
            if (outFile.exists()) {
                System.out.println(friendlyName());
                System.out.println("You cannot overwrite this file. If you "
                        + "wish to continue, delete the file " + "manually.");

                return;
            }

            responseWriter = new JsonWriter(new FileWriter(outFile));

            //responseWriter.setIndent("");
            // to pretty print, use this one instead
            responseWriter.setIndent("  ");
        } catch (IOException e) {
            throw new RuntimeException("Log files could not be created: "
                    + e.getMessage());
        }
        System.out.println("Putting files at " + outFile.getAbsolutePath());

        try {
            responseWriter.beginObject()
                          .name("responses")
                          .beginArray();
        } catch (IOException e) {
            throw new RuntimeException("Log file header could not be written: "
                    + e.getMessage());
        }
        
        lastPopup = 0;
    }

    @Override
    public void process(IGazeResponse response) {
        lastGazeResponse = response;
        if(lastPopup + SECONDS_BETWEEN_POPUP * 1000 <= response.getGaze().getSystemTime()) {
            lastPopup = response.getGaze().getSystemTime();
            // TODO: Only create the thread once and reuse it.
            new EmotionPopupWindow(this, response).start();
        }
    }

    @Override
    public void dispose() {
        try {
            responseWriter.endArray()
                          .endObject();
            responseWriter.flush();
            responseWriter.close();
            System.out.println("Emotion responses saved.");
        } catch (IOException e) {
            throw new RuntimeException("Log file footer could not be written: "
                    + e.getMessage());
        }
        outFile = null;
    }

    @Override
    public void config(String sessionID, String devUsername) {
    	filename = "emotion-responses-" + devUsername +
    			"-" + sessionID + ".json";
    	this.sessionID = sessionID;
    }
    public void config(String sessionID) {
    	filename = "emotion-responses-" + sessionID + ".json";
    	this.sessionID = sessionID;
    }
    
    public String getFilename() {
        String workspaceLocation =
                ResourcesPlugin.getWorkspace().getRoot().getLocation()
                        .toString();
        return workspaceLocation + "/" + filename;
    }

    @Override
    public void displayExportFile() {
    	JTextField displayVal = new JTextField(filename);
    	displayVal.setEditable(false);
    	
    	JPanel displayPanel = new JPanel();
    	displayPanel.setLayout(new BoxLayout(displayPanel, BoxLayout.Y_AXIS)); //vertically align
    	displayPanel.add(new JLabel("Export Filename"));
    	displayPanel.add(displayVal);
    	displayPanel.setPreferredSize(new Dimension(400,40)); //resize appropriately
    	
    	final int displayDialog = JOptionPane.showConfirmDialog(null, displayPanel, 
    			friendlyName() + " Display", JOptionPane.OK_CANCEL_OPTION,
    			JOptionPane.PLAIN_MESSAGE);
    	if (displayDialog == JOptionPane.OK_OPTION) {
    		//do nothing
    	}
    }

	public void handleEvent(Event event) {
		String[] propertyNames = event.getPropertyNames();
		IGazeResponse response = (IGazeResponse)event.getProperty(propertyNames[0]);
		this.process(response);
	}

    public String friendlyName() {
        return "Emotion Popup";
    }
    
    public void disable() {
    	PlatformUI.getWorkbench().getService(IEventBroker.class).unsubscribe(this);
    }
    
    public void enable() {
    	PlatformUI.getWorkbench().getService(IEventBroker.class).subscribe("iTrace/emotionPopup", this);
    }

    public void writeResponse(IGazeResponse response, String emotion, String[] options) {
        try {
                responseWriter.beginObject()    
//                              .name("popup_session_time")
//                              .value(response.getGaze().getSessionTime())
                              .name("popup_event_time")
                              .value(response.getGaze().getEventTime())
                              .name("popup_system_time")
                              .value(response.getGaze().getSystemTime())
//                              .name("response_session_time")
//                              .value(lastGazeResponse.getGaze().getSessionTime())
                              .name("response_event_time")
                              .value(lastGazeResponse.getGaze().getEventTime())
                              .name("response_system_time")
                              .value(lastGazeResponse.getGaze().getSystemTime())
                              .name("selected_emotion")
                              .value(emotion)
                              .name("emotion_display_order")
                              .beginArray();
                
                for(String option : options) {
                    responseWriter.value(option);
                }
                
                responseWriter.endArray()
                              .endObject();
        } catch (IOException e) {
            // ignore write errors
        }
    }
}