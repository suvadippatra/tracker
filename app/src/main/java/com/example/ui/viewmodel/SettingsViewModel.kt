package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.AppDatabase
import com.example.utils.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch

class SettingsViewModel(application: Application) : AndroidViewModel(application) {

    private val db = AppDatabase.getDatabase(application)
    private val syncEngine = SyncEngine(db)

    // Current Analysis State
    private val _isAnalyzing = MutableStateFlow(false)
    val isAnalyzing: StateFlow<Boolean> = _isAnalyzing.asStateFlow()

    private val _rawImportJson = MutableStateFlow<String?>(null)
    val rawImportJson: StateFlow<String?> = _rawImportJson.asStateFlow()

    private val _syncAnalysis = MutableStateFlow<SyncAnalysisResult?>(null)
    val syncAnalysis: StateFlow<SyncAnalysisResult?> = _syncAnalysis.asStateFlow()

    // Phase 3 & 4 UI Decision States
    private val _parentDecisions = MutableStateFlow<Map<String, ParentResolution>>(emptyMap())
    val parentDecisions: StateFlow<Map<String, ParentResolution>> = _parentDecisions.asStateFlow()

    private val _acceptedMenuItemKeys = MutableStateFlow<Set<String>>(emptySet())
    val acceptedMenuItemKeys: StateFlow<Set<String>> = _acceptedMenuItemKeys.asStateFlow()

    private val _conflictDecisions = MutableStateFlow<Map<String, ConflictResolution>>(emptyMap())
    val conflictDecisions: StateFlow<Map<String, ConflictResolution>> = _conflictDecisions.asStateFlow()

    private val _finalMergePlan = MutableStateFlow<MergePlan?>(null)
    val finalMergePlan: StateFlow<MergePlan?> = _finalMergePlan.asStateFlow()

    private val _errorMessage = MutableStateFlow<String?>(null)
    val errorMessage: StateFlow<String?> = _errorMessage.asStateFlow()

    // Import Flow State Machine
    sealed class ImportUiState {
        object Idle : ImportUiState()
        object Analyzing : ImportUiState()
        data class ParentMappingRequired(val unmappedParents: List<UnmappedParent>) : ImportUiState()
        data class MenuConfirmationRequired(val pendingMenuItems: List<PendingNewMenuItem>) : ImportUiState()
        data class ConflictsDetected(val conflicts: List<ConflictItem>) : ImportUiState()
        data class ReadyToMerge(val plan: MergePlan) : ImportUiState()
        object Merging : ImportUiState()
        data class Completed(val message: String) : ImportUiState()
    }

    private val _importUiState = MutableStateFlow<ImportUiState>(ImportUiState.Idle)
    val importUiState: StateFlow<ImportUiState> = _importUiState.asStateFlow()

    /**
     * Step 1: Parse and analyze imported JSON without modifying the database.
     */
    fun startImportAnalysis(
        jsonString: String,
        targetHotelId: Int? = null,
        targetBankAccountId: Int? = null,
        explicitSenderName: String? = null,
        onComplete: (SyncAnalysisResult) -> Unit = {}
    ) {
        viewModelScope.launch {
            try {
                _isAnalyzing.value = true
                _errorMessage.value = null
                _rawImportJson.value = jsonString

                val analysis = syncEngine.analyzeImport(
                    jsonString = jsonString,
                    targetHotelId = targetHotelId,
                    targetBankAccountId = targetBankAccountId,
                    explicitSenderName = explicitSenderName
                )

                _syncAnalysis.value = analysis

                // Default all new menu items to accepted
                _acceptedMenuItemKeys.value = analysis.pendingNewMenuItems.map { it.itemKey }.toSet()

                // Default all conflicts to "Take Latest"
                _conflictDecisions.value = syncEngine.autoResolveAllLatest(analysis.conflicts)

                // Initialize parent decisions if any
                val initParentDecisions = mutableMapOf<String, ParentResolution>()
                analysis.unmappedParents.forEach { parent ->
                    initParentDecisions[parent.entityKey] = ParentResolution.CreateNew(analysis.senderName)
                }
                _parentDecisions.value = initParentDecisions

                _isAnalyzing.value = false
                
                // Determine next step in state machine
                transitionToNextImportStep(analysis)
                onComplete(analysis)
            } catch (e: Exception) {
                _isAnalyzing.value = false
                _importUiState.value = ImportUiState.Idle
                _errorMessage.value = "Failed to analyze import payload: ${e.message}"
                e.printStackTrace()
            }
        }
    }

    /**
     * Determines which step of the interactive import flow to show next.
     */
    fun transitionToNextImportStep(analysis: SyncAnalysisResult? = _syncAnalysis.value) {
        val currAnalysis = analysis ?: run {
            _importUiState.value = ImportUiState.Idle
            return
        }

        // 1. If unmapped parents exist, show Parent Mapping dialog
        if (currAnalysis.unmappedParents.isNotEmpty()) {
            _importUiState.value = ImportUiState.ParentMappingRequired(currAnalysis.unmappedParents)
            return
        }

        // 2. If new menu items exist, show Menu Confirmation dialog
        if (currAnalysis.pendingNewMenuItems.isNotEmpty()) {
            _importUiState.value = ImportUiState.MenuConfirmationRequired(currAnalysis.pendingNewMenuItems)
            return
        }

        // 3. If conflicts exist, show Conflicts resolution
        if (currAnalysis.conflicts.isNotEmpty()) {
            _importUiState.value = ImportUiState.ConflictsDetected(currAnalysis.conflicts)
            return
        }

        // 4. Otherwise, ready to merge directly
        val plan = generateMergePlan()
        if (plan != null) {
            _importUiState.value = ImportUiState.ReadyToMerge(plan)
        } else {
            _importUiState.value = ImportUiState.Idle
        }
    }

    /**
     * Called when user confirms parent mapping dialog.
     * Transitions to Menu Confirmation (if pending menus exist) or Conflicts.
     */
    fun confirmParentMappings() {
        val currAnalysis = _syncAnalysis.value ?: return
        if (currAnalysis.pendingNewMenuItems.isNotEmpty()) {
            _importUiState.value = ImportUiState.MenuConfirmationRequired(currAnalysis.pendingNewMenuItems)
        } else if (currAnalysis.conflicts.isNotEmpty()) {
            _importUiState.value = ImportUiState.ConflictsDetected(currAnalysis.conflicts)
        } else {
            val plan = generateMergePlan()
            if (plan != null) {
                _importUiState.value = ImportUiState.ReadyToMerge(plan)
            } else {
                _importUiState.value = ImportUiState.Idle
            }
        }
    }

    /**
     * Called when user confirms new menu items dialog.
     * Transitions to Conflicts or Direct Merge.
     */
    fun confirmMenuItems() {
        val currAnalysis = _syncAnalysis.value ?: return
        if (currAnalysis.conflicts.isNotEmpty()) {
            _importUiState.value = ImportUiState.ConflictsDetected(currAnalysis.conflicts)
        } else {
            val plan = generateMergePlan()
            if (plan != null) {
                _importUiState.value = ImportUiState.ReadyToMerge(plan)
            } else {
                _importUiState.value = ImportUiState.Idle
            }
        }
    }

    /**
     * Sets user decision for an unmapped parent entity (Hotel, Bank Account, Owner Account).
     */
    fun setParentResolution(entityKey: String, resolution: ParentResolution) {
        val updated = _parentDecisions.value.toMutableMap()
        updated[entityKey] = resolution
        _parentDecisions.value = updated
        recalculateWithDecisions()
    }

    /**
     * Re-runs analysis to apply parent resolutions and recalculate child conflicts.
     */
    fun recalculateWithDecisions() {
        val currentAnalysis = _syncAnalysis.value ?: return
        viewModelScope.launch {
            try {
                val updatedAnalysis = syncEngine.resolveParentDecisions(
                    previousAnalysis = currentAnalysis,
                    parentDecisions = _parentDecisions.value
                )
                _syncAnalysis.value = updatedAnalysis

                // Preserve or update conflict decisions
                val latestConflictMap = syncEngine.autoResolveAllLatest(updatedAnalysis.conflicts)
                val mergedConflicts = latestConflictMap.toMutableMap()
                _conflictDecisions.value.forEach { (k, v) ->
                    if (mergedConflicts.containsKey(k)) {
                        mergedConflicts[k] = v
                    }
                }
                _conflictDecisions.value = mergedConflicts

                // Update accepted menu item keys
                val newKeys = updatedAnalysis.pendingNewMenuItems.map { it.itemKey }.toSet()
                _acceptedMenuItemKeys.value = _acceptedMenuItemKeys.value.intersect(newKeys).ifEmpty { newKeys }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
    }

    /**
     * Toggle acceptance of a newly discovered menu item.
     */
    fun setMenuItemAccepted(itemKey: String, accepted: Boolean) {
        val current = _acceptedMenuItemKeys.value.toMutableSet()
        if (accepted) {
            current.add(itemKey)
        } else {
            current.remove(itemKey)
        }
        _acceptedMenuItemKeys.value = current
    }

    fun setAllMenuItemsAccepted(accepted: Boolean) {
        val currentAnalysis = _syncAnalysis.value ?: return
        if (accepted) {
            _acceptedMenuItemKeys.value = currentAnalysis.pendingNewMenuItems.map { it.itemKey }.toSet()
        } else {
            _acceptedMenuItemKeys.value = emptySet()
        }
    }

    /**
     * Set decision for a specific conflict item.
     */
    fun setConflictResolution(conflictKey: String, resolution: ConflictResolution) {
        val current = _conflictDecisions.value.toMutableMap()
        current[conflictKey] = resolution
        _conflictDecisions.value = current
    }

    /**
     * Quick action: Resolve all conflicts to "Take Latest" (timestamp comparison).
     */
    fun resolveAllConflictsLatest() {
        val currentAnalysis = _syncAnalysis.value ?: return
        _conflictDecisions.value = syncEngine.autoResolveAllLatest(currentAnalysis.conflicts)
    }

    /**
     * Quick action: Resolve all conflicts to "Keep Local".
     */
    fun resolveAllConflictsLocal() {
        val currentAnalysis = _syncAnalysis.value ?: return
        _conflictDecisions.value = syncEngine.autoResolveAllLocal(currentAnalysis.conflicts)
    }

    /**
     * Compiles the final concrete MergePlan ready for Room execution.
     */
    fun generateMergePlan(): MergePlan? {
        val currentAnalysis = _syncAnalysis.value ?: return null
        val plan = syncEngine.buildFinalMergePlan(
            analysis = currentAnalysis,
            acceptedMenuItemKeys = _acceptedMenuItemKeys.value,
            conflictResolutions = _conflictDecisions.value
        )
        _finalMergePlan.value = plan
        return plan
    }

    /**
     * Executes the merge plan inside Room database atomically.
     */
    fun executeMergePlan(
        plan: MergePlan,
        onResult: (com.example.data.ImportOutcome) -> Unit
    ) {
        viewModelScope.launch {
            try {
                _importUiState.value = ImportUiState.Merging
                syncEngine.executeMergePlan(plan)
                val totalInserted = plan.hotelsToInsert.size + plan.bankAccountsToInsert.size + plan.ownerAccountsToInsert.size + plan.menusToInsert.size + plan.mealsToInsert.size + plan.expensesToInsert.size + plan.readingsToInsert.size + plan.billsToInsert.size
                val totalUpdated = plan.hotelsToUpdate.size + plan.bankAccountsToUpdate.size + plan.menusToUpdate.size + plan.mealsToUpdate.size + plan.expensesToUpdate.size + plan.readingsToUpdate.size + plan.billsToUpdate.size
                val successMessage = "Data synchronized successfully! ($totalInserted new records added, $totalUpdated records updated)"
                _importUiState.value = ImportUiState.Completed(successMessage)
                clearImportSession()
                onResult(com.example.data.ImportOutcome.Success(successMessage))
            } catch (e: Exception) {
                _importUiState.value = ImportUiState.Idle
                _errorMessage.value = "Merge failed: ${e.message}"
                e.printStackTrace()
                onResult(com.example.data.ImportOutcome.Error("Merge error: ${e.message}"))
            }
        }
    }

    fun cancelImportFlow() {
        _importUiState.value = ImportUiState.Idle
        clearImportSession()
    }

    /**
     * Clears all state and resets analyzer.
     */
    fun clearImportSession() {
        _rawImportJson.value = null
        _syncAnalysis.value = null
        _parentDecisions.value = emptyMap()
        _acceptedMenuItemKeys.value = emptySet()
        _conflictDecisions.value = emptyMap()
        _finalMergePlan.value = null
        _errorMessage.value = null
    }
}
