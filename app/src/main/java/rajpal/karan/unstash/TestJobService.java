package rajpal.karan.unstash;

import android.app.job.JobParameters;
import android.app.job.JobService;
import android.content.Intent;
import android.os.Build;
import android.support.annotation.RequiresApi;

import timber.log.Timber;

public class TestJobService extends JobService {
    @RequiresApi(api = Build.VERSION_CODES.M)
    @Override
    public boolean onStartJob(JobParameters jobParameters) {
        Timber.d("Starting JobService");
        Intent service = new Intent(getApplicationContext(), UnstashFetchService.class);
        getApplicationContext().startService(service);
        Utils.scheduleJob(getApplicationContext()); // reschedule the job
        return true;
    }

    @Override
    public boolean onStopJob(JobParameters jobParameters) {
        return true;
    }
}
