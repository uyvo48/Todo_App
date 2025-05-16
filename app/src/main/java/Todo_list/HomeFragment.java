package Todo_list;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import Adapter.HomeTaskAdapter;
import model.TaskModel;
import authentication.LoginScreen;
import com.example.todo_app.R;

public class HomeFragment extends Fragment {
    private ArrayList<TaskModel> taskList;
    private HomeTaskAdapter adapter;
    private ListView listView;

    private CollectionReference tasksRef;
    private FirebaseAuth mAuth;
    private String userId;

    public HomeFragment() {
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        int resultCode = GoogleApiAvailability.getInstance().isGooglePlayServicesAvailable(requireContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("HomeFragment", "Google Play Services error: " + resultCode);
            if (isAdded()) {
                GoogleApiAvailability.getInstance().getErrorDialog(requireActivity(), resultCode, 0).show();
            }
            return;
        }

        taskList = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            startActivity(new Intent(requireContext(), LoginScreen.class));
            requireActivity().finish();
            return;
        }

        userId = currentUser.getUid();
        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("users").document(userId).collection("tasks");

        adapter = new HomeTaskAdapter(requireContext(), taskList, this::openTaskFragment, userId);
        loadTasksFromFirestore();
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_home, container, false);

        listView = view.findViewById(R.id.list_item_task);
        ImageButton addButton = view.findViewById(R.id.btnAdd_home);

        if (listView == null || addButton == null) {
            Log.e("HomeFragment", "One or more views not found");
            return view;
        }

        listView.setAdapter(adapter);

        addButton.setOnClickListener(v -> openTaskFragment(null, null));

        return view;
    }

    private void loadTasksFromFirestore() {
        if (tasksRef == null) {
            Log.d("HomeFragment", "tasksRef is null, skipping load");
            return;
        }

        tasksRef.get().addOnSuccessListener(querySnapshot -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() -> {
                    taskList.clear();
                    if (querySnapshot.isEmpty()) {
                        Log.d("HomeFragment", "No tasks in Firestore");
                        Toast.makeText(requireContext(), "No tasks to display", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            TaskModel task = document.toObject(TaskModel.class);
                            task.setId(document.getId());
                            taskList.add(task);
                            Log.d("HomeFragment", "Loaded task: id=" + document.getId() + ", title=" + task.getTitle());
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                        Log.d("HomeFragment", "Tasks loaded: " + taskList.size());
                    } else {
                        Log.e("HomeFragment", "Adapter is null when loading tasks");
                    }
                });
            }
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
            Log.e("HomeFragment", "Failed to load tasks: " + e.getMessage());
        });
    }

    private void openTaskFragment(String taskId, String title) {
        TaskFragment taskFragment = TaskFragment.newInstance(title != null ? title : "", taskId != null ? taskId : "");
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, taskFragment)
                .addToBackStack(null)
                .commit();
    }
}