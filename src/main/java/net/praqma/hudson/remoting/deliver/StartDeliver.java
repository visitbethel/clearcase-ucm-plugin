package net.praqma.hudson.remoting.deliver;

import hudson.FilePath;
import hudson.model.BuildListener;
import hudson.remoting.VirtualChannel;
import net.praqma.clearcase.Deliver;
import net.praqma.clearcase.exceptions.ClearCaseException;
import net.praqma.clearcase.exceptions.CleartoolException;
import net.praqma.clearcase.exceptions.DeliverException;
import net.praqma.clearcase.ucm.entities.Activity;
import net.praqma.clearcase.ucm.entities.Baseline;
import net.praqma.clearcase.ucm.entities.Stream;
import net.praqma.clearcase.ucm.entities.Version;
import net.praqma.clearcase.ucm.view.SnapshotView;
import net.praqma.hudson.Config;
import net.praqma.hudson.exception.DeliverNotCancelledException;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.util.List;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * @author cwolfgang
 *
 * @since 1.4.0
 */
public class StartDeliver implements FilePath.FileCallable<Boolean> {

    private static Logger logger;

    private BuildListener listener;
    private Stream destinationStream;
    private Baseline baseline;
    private SnapshotView snapview;
    private String loadModule;
    private boolean forceDeliver;
    private boolean swipe;

    public StartDeliver( BuildListener listener, Stream destinationStream, Baseline baseline, SnapshotView snapview, String loadModule, boolean forceDeliver, boolean swipe ) {
        this.listener = listener;
        this.destinationStream = destinationStream;
        this.baseline = baseline;
        this.snapview = snapview;
        this.loadModule = loadModule;
        this.forceDeliver = forceDeliver;
        this.swipe = swipe;
    }


    @Override
    public Boolean invoke( File workspace, VirtualChannel channel ) throws IOException, InterruptedException {

        logger = Logger.getLogger( StartDeliver.class.getName() );
        logger.fine( "Start deliver" );

        try {
            deliver( baseline, destinationStream, forceDeliver, 2 );
            return true;
        } catch( Exception e ) {
            throw new IOException( "Error while starting deliver", e );
        }
    }

    private void deliver( Baseline baseline, Stream dstream, boolean forceDeliver, int triesLeft ) throws IOException, DeliverNotCancelledException, ClearCaseException {

        PrintStream out = listener.getLogger();

        logger.config( "Delivering " + baseline.getShortname() + " to " + dstream.getShortname() + ". Tries left: " + triesLeft );
        if( triesLeft < 1 ) {
            out.println( "[" + Config.nameShort + "] Unable to deliver, giving up." );
            throw new DeliverNotCancelledException( "Unable to force cancel deliver" );
        }

        Deliver deliver = null;
        try {
            out.println( "[" + Config.nameShort + "] Starting deliver(tries left: " + triesLeft + ")" );
            deliver = new Deliver( baseline, baseline.getStream(), dstream, snapview.getViewRoot(), snapview.getViewtag() );
            deliver.deliver( true, false, true, false );

        } catch( DeliverException e ) {
            logger.log( Level.FINE, "Failed to deliver", e );

            if( e.getType().equals( DeliverException.Type.DELIVER_IN_PROGRESS ) ) {
                out.println( "[" + Config.nameShort + "] Deliver already in progress" );

                if( forceDeliver ) {

                    out.println( e.getMessage() );
                    out.println( "[" + Config.nameShort + "] Forcing this deliver." );

                    try {
                        Deliver.cancel( dstream );
                        snapview.Update( swipe, true, true, false, new SnapshotView.LoadRules( snapview, SnapshotView.Components.valueOf( loadModule.toUpperCase() ) ) );

                    } catch( ClearCaseException ex ) {
                        throw ex;
                    }

                    // Recursive method call of INVOKE(...);
                    logger.config( "Trying to deliver again..." );
                    deliver( baseline, dstream, forceDeliver, triesLeft - 1 );

				/* Not forcing this deliver */
                } else {
                    throw e;
                }

			/* Another deliver is not in progress */
            } else {
                throw e;
            }
        } catch( CleartoolException e ) {
            logger.warning( "Unable to get status from stream: " + e.getMessage() );
            throw new IOException( e );
        } catch( Exception e ) {
            logger.warning( "Unable deliver: " + e.getMessage() );

            throw new IOException( e );
        }
    }
}
