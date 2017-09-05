package rajpal.karan.unstash;

import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import com.firebase.jobdispatcher.JobParameters;
import com.firebase.jobdispatcher.JobService;

import timber.log.Timber;

public class TestJobService extends JobService {
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Timber.d("Starting JobService");
        Intent service = new Intent(getApplicationContext(), UnstashFetchService.class);
        getApplicationContext().startService(service);
        Utils.scheduleReadPostReminder(getApplicationContext(), null); // reschedule the job
        NotificationUtils.executeTask(this, UnstashFetchService.ACTION_READ_POST_REMINDER);
        jobFinished(jobParameters, false);
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        // Called when specified conditions are no longer met
        // Can kill the service here when user isn't on wifi or charging
        return true;
    }
}
