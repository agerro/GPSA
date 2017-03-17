package se.devner.gpsa;

import android.app.AlertDialog;
import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.DialogInterface;
import android.os.AsyncTask;
import android.os.Handler;
import android.os.Message;
import android.util.Log;
import android.widget.Toast;

import static se.devner.gpsa.MainActivity.clickedMarker;
import static se.devner.gpsa.MainActivity.map;
import static se.devner.gpsa.MainActivity.notifying;
import static se.devner.gpsa.MainActivity.stopCheck;
import static se.devner.gpsa.MainActivity.tb;

/**
 * Created by paulruiz on 3/7/15.
 */
public class JobSchedulerService extends JobService {

    private UpdateAppsAsyncTask updateTask = new UpdateAppsAsyncTask();

    private Handler mJobHandler = new Handler( new Handler.Callback() {


        @Override
        public boolean handleMessage( Message msg ) {
            Toast.makeText( getApplicationContext(), "JobService task running", Toast.LENGTH_SHORT ).show();
            jobFinished( (JobParameters) msg.obj, false );
            return true;
        }
    } );

    @Override
    public boolean onStartJob(JobParameters params ) {
                    Log.d("FoundedTrueIs", "true");
        updateTask.execute(params);

        mJobHandler.sendMessage( Message.obtain( mJobHandler, 1, params ) );
        return true;
    }

    @Override
    public boolean onStopJob( JobParameters params ) {
        mJobHandler.removeMessages( 1 );
        boolean shouldReschedule = updateTask.stopJob(params);

        stopCheck();
        return shouldReschedule;

    }

    private class UpdateAppsAsyncTask extends AsyncTask<JobParameters, Void, JobParameters[]> {


        @Override
        protected JobParameters[] doInBackground(JobParameters... params) {

            if (MainActivity.currentLocation != null) {
                if (MainActivity.userWithinRange()) {
                    //Notify
                    if(!notifying) {
                        //MainActivity.startNotification();
                        MainActivity.stopCheck();
                    }
                }
            }
            // Do updating and stopping logical here.
            Log.d("InService", "Updating apps in the background");
            return params;
        }

        @Override
        protected void onPostExecute(JobParameters[] result) {
            for (JobParameters params : result) {
                if (!hasJobBeenStopped(params)) {
                    Log.d("ErrorInService", "finishing job with id=" + params.getJobId());
                    jobFinished(params, false);
                }
            }
        }

        private boolean hasJobBeenStopped(JobParameters params) {
            // Logic for checking stop.
            return false;
        }

        public boolean stopJob(JobParameters params) {
            Log.d("ErrorInService", "stopJob id=" + params.getJobId());
            // Logic for stopping a job. return true if job should be rescheduled.
            return false;
        }

    }
}