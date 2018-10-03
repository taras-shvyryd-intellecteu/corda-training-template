package net.corda.training.contract

import net.corda.core.contracts.*
import net.corda.core.transactions.LedgerTransaction
import net.corda.training.state.IOUState

/**
 * This is where you'll add the contract code which defines how the [IOUState] behaves. Looks at the unit tests in
 * [IOUContractTests] for instructions on how to complete the [IOUContract] class.
 */
class IOUContract : Contract {
    companion object {
        @JvmStatic
        val IOU_CONTRACT_ID = "net.corda.training.contract.IOUContract"
    }

    /**
     * Add any commands required for this contract as classes within this interface.
     * It is useful to encapsulate your commands inside an interface, so you can use the [requireSingleCommand]
     * function to check for a number of commands which implement this interface.
     */
    interface Commands : CommandData {
        // Add commands here.
        // E.g
        // class DoSomething : TypeOnlyCommandData(), Commands
        class Issue : TypeOnlyCommandData(), Commands
        class Transfer : TypeOnlyCommandData(), Commands
    }

    /**
     * The contract code for the [IOUContract].
     * The constraints are self documenting so don't require any additional explanation.
     */
    override fun verify(tx: LedgerTransaction) {
        // Add contract code here.
        val command = tx.commands.requireSingleCommand<Commands>()
        when (command.value) {
            is Commands.Issue -> requireThat {
                "No inputs should be consumed when issuing an IOU." using (tx.inputStates.size == 0)
                "Only one output state should be created when issuing an IOU." using (tx.outputStates.size == 1)
                val state = tx.outputStates.single() as IOUState
                "A newly issued IOU must have a positive amount." using (state.amount.quantity > 0)
                "The lender and borrower cannot have the same identity." using (state.borrower != state.lender)
                "Both lender and borrower together only may sign IOU issue transaction." using (command.signers.toSet() == state.participants.map { it.owningKey }.toSet())
            }
            is Commands.Transfer -> requireThat {
                "An IOU transfer transaction should only consume one input state." using (tx.inputStates.size == 1)
                "An IOU transfer transaction should only create one output state." using (tx.outputStates.size == 1)
                val input = tx.inputStates.single() as IOUState
                val output = tx.outputStates.single() as IOUState
                "Only the lender property may change." using (input == output.withNewLender(input.lender))
                "The lender property must change in a transfer." using (input.lender != output.lender)
                "The borrower, old lender and new lender only must sign an IOU transfer transaction" using (command.signers.toSet() == input.participants.map { it.owningKey }.union(output.participants.map { it.owningKey }).toSet())
            }
        }
    }
}
