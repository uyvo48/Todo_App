package Todo_list;

import android.content.Intent;
import android.os.Bundle;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.android.material.textfield.TextInputEditText;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import com.google.firebase.firestore.CollectionReference;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;

import Adapter.TaskAdapter;
import model.SubTaskModel;
import model.TaskModel;
import authentication.LoginScreen;
import com.example.todo_app.R;

public class TaskFragment extends Fragment {
    private static final String ARG_PARAM1 = "param1"; // title
    private static final String ARG_PARAM2 = "param2"; // taskId
    private static final String KEY_CURRENT_TITLE = "current_title";
    private static final String KEY_TASK_ID = "task_id";
    private static final String KEY_SUBTASKS_REF_PATH = "subtasks_ref_path";
    private static final int REQUEST_GOOGLE_PLAY_SERVICES = 1000;

    private String taskId;
    private String currentTitle;

    private ArrayList<SubTaskModel> subTaskList;
    private TaskAdapter adapter;
    private ListView listView;

    private CollectionReference tasksRef;
    private CollectionReference subTasksRef;

    private FirebaseAuth mAuth;
    private TaskModel currentTask;

    public TaskFragment() {
    }

    public static TaskFragment newInstance(String param1, String param2) {
        TaskFragment fragment = new TaskFragment();
        Bundle args = new Bundle();
        args.putString(ARG_PARAM1, param1);
        args.putString(ARG_PARAM2, param2);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setRetainInstance(true);

        if (getArguments() != null) {
            taskId = getArguments().getString(ARG_PARAM2, "");
            currentTitle = getArguments().getString(ARG_PARAM1, "");
            Log.d("TaskFragment", "Received title from HomeFragment: " + currentTitle + ", taskId: " + taskId);
        } else {
            Log.w("TaskFragment", "Arguments are null, checking lifecycle");
        }

        GoogleApiAvailability googleApiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = googleApiAvailability.isGooglePlayServicesAvailable(requireContext());
        if (resultCode != ConnectionResult.SUCCESS) {
            Log.e("TaskFragment", "Google Play Services error: " + resultCode);
            if (isAdded()) {
                if (googleApiAvailability.isUserResolvableError(resultCode)) {
                    googleApiAvailability.getErrorDialog(requireActivity(), resultCode, REQUEST_GOOGLE_PLAY_SERVICES).show();
                } else {
                    Toast.makeText(requireContext(), "This device does not support Google Play Services", Toast.LENGTH_LONG).show();
                    startActivity(new Intent(requireContext(), LoginScreen.class));
                    requireActivity().finish();
                }
            }
            return;
        }

        subTaskList = new ArrayList<>();
        mAuth = FirebaseAuth.getInstance();
        FirebaseUser currentUser = mAuth.getCurrentUser();
        if (currentUser == null) {
            Log.e("TaskFragment", "No authenticated user found, redirecting to LoginScreen");
            startActivity(new Intent(requireContext(), LoginScreen.class));
            requireActivity().finish();
            return;
        }

        FirebaseFirestore db = FirebaseFirestore.getInstance();
        tasksRef = db.collection("users").document(currentUser.getUid()).collection("tasks");

        if (savedInstanceState != null) {
            String savedTaskId = savedInstanceState.getString(KEY_TASK_ID, "");
            String savedTitle = savedInstanceState.getString(KEY_CURRENT_TITLE, "");
            if (!TextUtils.isEmpty(savedTaskId)) taskId = savedTaskId;
            if (!TextUtils.isEmpty(savedTitle)) currentTitle = savedTitle;
            String subTasksRefPath = savedInstanceState.getString(KEY_SUBTASKS_REF_PATH);
            if (subTasksRefPath != null) {
                subTasksRef = db.collection(subTasksRefPath);
            }
            Log.d("TaskFragment", "Restored from savedInstanceState: currentTitle=" + currentTitle + ", taskId=" + taskId);
        }

        if (!TextUtils.isEmpty(taskId)) {
            subTasksRef = tasksRef.document(taskId).collection("subTasks");
            Log.d("TaskFragment", "Initialized subTasksRef with taskId: " + taskId);
        } else {
            Log.d("TaskFragment", "taskId is empty, this is a new task");
        }

        adapter = new TaskAdapter(requireContext(), subTaskList, currentTitle, subTasksRef);
        Log.d("TaskFragment", "Initialized adapter in onCreate with subTasksRef: " + (subTasksRef != null ? "not null" : "null"));
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putString(KEY_CURRENT_TITLE, currentTitle);
        outState.putString(KEY_TASK_ID, taskId);
        if (subTasksRef != null) {
            outState.putString(KEY_SUBTASKS_REF_PATH, subTasksRef.getPath());
        }
        Log.d("TaskFragment", "Saved state: currentTitle=" + currentTitle + ", taskId=" + taskId);
    }

    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, ViewGroup container, Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_task, container, false);

        TextInputEditText taskInputTitle = view.findViewById(R.id.textFieldTitle);
        ImageButton addButton = view.findViewById(R.id.btnAdd_Task);
        Button saveButton = view.findViewById(R.id.button);
        listView = view.findViewById(R.id.list_item_task);
        TextView taskMainTitle = view.findViewById(R.id.task_main_title);

        if (taskInputTitle == null || addButton == null || listView == null || saveButton == null || taskMainTitle == null) {
            Log.e("TaskFragment", "One or more views not found");
            return view;
        }

        adapter = new TaskAdapter(requireContext(), subTaskList, currentTitle, subTasksRef);
        listView.setAdapter(adapter);
        Log.d("TaskFragment", "Adapter initialized and set to ListView");

        if (!TextUtils.isEmpty(currentTitle)) {
            taskMainTitle.setText(currentTitle);
            taskMainTitle.setVisibility(View.VISIBLE);
            taskInputTitle.setText(currentTitle);
            Log.d("TaskFragment", "Displayed title in onCreateView: " + currentTitle);
        } else {
            taskMainTitle.setVisibility(View.GONE);
            Log.d("TaskFragment", "currentTitle is empty in onCreateView");
        }

        if (subTasksRef != null && !TextUtils.isEmpty(taskId)) {
            Log.d("TaskFragment", "Loading sub-tasks for taskId: " + taskId);
            loadSubTasksFromFirestore();
        } else {
            Log.d("TaskFragment", "subTasksRef or taskId is null/empty, skipping sub-task load");
        }

        addButton.setOnClickListener(v -> {
            String title = taskInputTitle.getText() != null ? taskInputTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            currentTitle = title;
            taskMainTitle.setText(title);
            taskMainTitle.setVisibility(View.VISIBLE);
            taskInputTitle.setText(title);

            addNewSubTask();
        });

        saveButton.setOnClickListener(v -> {
            String title = taskInputTitle.getText() != null ? taskInputTitle.getText().toString().trim() : "";
            if (TextUtils.isEmpty(title)) {
                Toast.makeText(requireContext(), "Please enter a title", Toast.LENGTH_SHORT).show();
                return;
            }

            currentTitle = title;
            currentTask = new TaskModel(title);

            if (TextUtils.isEmpty(taskId)) {
                DocumentReference newTaskRef = tasksRef.document();
                taskId = newTaskRef.getId();
                subTasksRef = tasksRef.document(taskId).collection("subTasks");
                newTaskRef.set(currentTask)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("TaskFragment", "Task created with taskId: " + taskId);
                            saveSubTasks();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TaskFragment", "Failed to create task: " + e.getMessage());
                            Toast.makeText(requireContext(), "Failed to create task", Toast.LENGTH_SHORT).show();
                        });
            } else {
                tasksRef.document(taskId).set(currentTask)
                        .addOnSuccessListener(aVoid -> {
                            Log.d("TaskFragment", "Task updated with taskId: " + taskId);
                            saveSubTasks();
                        })
                        .addOnFailureListener(e -> {
                            Log.e("TaskFragment", "Failed to update task: " + e.getMessage());
                            Toast.makeText(requireContext(), "Failed to update task", Toast.LENGTH_SHORT).show();
                        });
            }
        });

        ImageButton backButton = view.findViewById(R.id.btnBack);
        if (backButton != null) {
            backButton.setOnClickListener(v -> {
                // Thay thế fragment hiện tại bằng HomeFragment
                requireActivity().getSupportFragmentManager()
                        .beginTransaction()
                        .replace(R.id.frameLayout, new HomeFragment())
                        .commit();
                // Xóa toàn bộ back stack để đảm bảo trạng thái sạch
                requireActivity().getSupportFragmentManager().popBackStack(null, androidx.fragment.app.FragmentManager.POP_BACK_STACK_INCLUSIVE);
                // Cập nhật BottomNavigationView để chọn mục "Home"
                if (requireActivity() instanceof MainTodoList) {
                    ((MainTodoList) requireActivity()).updateNavigationSelection(R.id.Home);
                }
                Log.d("TaskFragment", "Back button pressed, navigated to HomeFragment");
            });
        } else {
            Log.e("TaskFragment", "BackButton not found");
        }

        return view;
    }

    private void addNewSubTask() {
        if (subTaskList == null) {
            subTaskList = new ArrayList<>();
        }
        String subTaskId = subTasksRef != null ? subTasksRef.document().getId() : FirebaseFirestore.getInstance().collection("temp").document().getId();
        SubTaskModel newSubTask = new SubTaskModel(subTaskId, "New Subtask", false);

        subTaskList.add(newSubTask);
        Log.d("TaskFragment", "Sub-task added to list: id=" + subTaskId + ", content=" + newSubTask.getContent());

        if (adapter != null) {
            adapter.notifyDataSetChanged();
            if (listView != null) {
                listView.invalidateViews();
            }
            Log.d("TaskFragment", "Adapter notified, subTaskList size: " + subTaskList.size());
        } else {
            Log.e("TaskFragment", "Adapter is null when adding subtask");
        }
    }

    private void saveSubTasks() {
        if (subTasksRef == null) {
            Log.e("TaskFragment", "subTasksRef is null");
            Toast.makeText(requireContext(), "Failed to save sub-tasks", Toast.LENGTH_SHORT).show();
            return;
        }

        for (SubTaskModel subTask : subTaskList) {
            if (!TextUtils.isEmpty(subTask.getContent()) && subTask.getId() != null) {
                subTasksRef.document(subTask.getId()).set(subTask)
                        .addOnSuccessListener(aVoid -> Log.d("TaskFragment", "Sub-task saved: " + subTask.getId()))
                        .addOnFailureListener(e -> Log.e("TaskFragment", "Failed to save sub-task: " + e.getMessage()));
            }
        }
        Toast.makeText(requireContext(), "Tasks saved", Toast.LENGTH_SHORT).show();
        requireActivity().getSupportFragmentManager().popBackStack();
        requireActivity().getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.frameLayout, new HomeFragment())
                .commit();
    }

    private void loadSubTasksFromFirestore() {
        if (subTasksRef == null || TextUtils.isEmpty(taskId)) {
            Log.e("TaskFragment", "subTasksRef or taskId is null/empty, cannot load sub-tasks. taskId: " + taskId);
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Cannot load sub-tasks: Invalid task ID", Toast.LENGTH_SHORT).show()
                );
            }
            return;
        }

        tasksRef.document(taskId).get().addOnSuccessListener(documentSnapshot -> {
            if (!documentSnapshot.exists()) {
                Log.e("TaskFragment", "Task does not exist in Firestore: " + taskId);
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Task does not exist", Toast.LENGTH_SHORT).show()
                    );
                }
                return;
            }

            if (TextUtils.isEmpty(currentTitle)) {
                currentTitle = documentSnapshot.getString("title");
                if (!TextUtils.isEmpty(currentTitle) && isAdded()) {
                    requireActivity().runOnUiThread(() -> {
                        TextView taskMainTitle = getView().findViewById(R.id.task_main_title);
                        TextInputEditText taskInputTitle = getView().findViewById(R.id.textFieldTitle);
                        if (taskMainTitle != null && taskInputTitle != null) {
                            taskMainTitle.setText(currentTitle);
                            taskMainTitle.setVisibility(View.VISIBLE);
                            taskInputTitle.setText(currentTitle);
                            Log.d("TaskFragment", "Updated title from Firestore: " + currentTitle);
                        }
                    });
                }
            }

            subTasksRef.get().addOnSuccessListener(querySnapshot -> {
                if (!isAdded()) {
                    Log.w("TaskFragment", "Fragment not attached, skipping UI update");
                    return;
                }
                requireActivity().runOnUiThread(() -> {
                    subTaskList.clear();
                    Log.d("TaskFragment", "Cleared subTaskList, loading new data for taskId: " + taskId);
                    if (querySnapshot.isEmpty()) {
                        Log.d("TaskFragment", "No sub-tasks found in Firestore for taskId: " + taskId);
                        Toast.makeText(requireContext(), "No sub-tasks to display", Toast.LENGTH_SHORT).show();
                    } else {
                        for (QueryDocumentSnapshot document : querySnapshot) {
                            SubTaskModel subTask = document.toObject(SubTaskModel.class);
                            subTask.setId(document.getId());
                            subTaskList.add(subTask);
                            Log.d("TaskFragment", "Loaded sub-task: id=" + subTask.getId() + ", content=" + subTask.getContent());
                        }
                    }
                    if (adapter != null) {
                        adapter.notifyDataSetChanged();
                        if (listView != null) {
                            listView.invalidateViews();
                            Log.d("TaskFragment", "Adapter notified, subTaskList size: " + subTaskList.size());
                        } else {
                            Log.e("TaskFragment", "ListView is null when updating sub-tasks");
                        }
                    } else {
                        Log.e("TaskFragment", "Adapter is null when updating sub-tasks");
                    }
                });
            }).addOnFailureListener(e -> {
                if (isAdded()) {
                    requireActivity().runOnUiThread(() ->
                            Toast.makeText(requireContext(), "Failed to load sub-tasks: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                    );
                }
                Log.e("TaskFragment", "Failed to load sub-tasks for taskId: " + taskId + ", error: " + e.getMessage());
            });
        }).addOnFailureListener(e -> {
            if (isAdded()) {
                requireActivity().runOnUiThread(() ->
                        Toast.makeText(requireContext(), "Failed to load task: " + e.getMessage(), Toast.LENGTH_SHORT).show()
                );
            }
            Log.e("TaskFragment", "Failed to check task existence for taskId: " + taskId + ", error: " + e.getMessage());
        });
    }

//    @Override
//    public void onActivityResult(int requestCode, int resultCode, @Nullable Intent data) {
//        super.onActivityResult(requestCode, resultCode, data);
//        if (requestCode == REQUEST_GOOGLE_PLAY_SERVICES) {
//            if (resultCode == RESULT_OK) {
//                Log.d("TaskFragment", "Google Play Services resolved, retrying initialization");
//                requireActivity().recreate();
//            } else {
//                Log.e("TaskFragment", "User cancelled Google Play Services resolution");
//                Toast.makeText(requireContext(), "Google Play Services is required for this app", Toast.LENGTH_LONG).show();
//                startActivity(new Intent(requireContext(), LoginScreen.class));
//                requireActivity().finish();
//            }
//        }
//    }
}